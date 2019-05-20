/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.npva;

import java.util.ArrayList;
import java.util.List;

import org.epics.pva.data.PVAArray;
import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAFloatArray;
import org.epics.pva.data.PVAIntArray;
import org.epics.pva.data.PVALongArray;
import org.epics.pva.data.PVANumber;
import org.epics.pva.data.PVAShortArray;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStringArray;
import org.epics.pva.data.PVAStructure;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayUInteger;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;

@SuppressWarnings("nls")
public class PVAStructureHelper
{
    public static VType getVType(final PVAStructure struct, final PVNameHelper name_helper) throws Exception
    {
        PVAStructure actual = struct;
        if (! name_helper.getField().equals("value"))
        {   // Fetch data from a sub-field
            final PVAData field = struct.get(name_helper.getField());
            if (field instanceof PVAStructure)
                actual = (PVAStructure) field;
            else if (field instanceof PVANumber)
                return Decoders.decodeNumber(struct, (PVANumber) field);
            if (field instanceof PVAString)
                return Decoders.decodeString(struct, (PVAString) field);
        }

        // Handle normative types
        String type = actual.getStructureName();
        if (type.startsWith("epics:nt/"))
            type = type.substring(9);
        if (type.equals("NTScalar:1.0"))
            return decodeScalar(actual);
        if (type.equals("NTEnum:1.0"))
            return Decoders.decodeEnum(actual);
        if (type.equals("NTScalarArray:1.0"))
            return decodeNTArray(actual);
        if (type.equals("NTNDArray:1.0"))
            return ImageDecoder.decode(actual);
        if (type.equals("NTTable:1.0"))
            return decodeNTTable(actual);

        // Handle data that contains a "value", even though not marked as NT*
        final PVAData field = actual.get("value");
        if (field instanceof PVANumber  ||
            field instanceof PVAString)
            return decodeScalar(actual);
        else if (field instanceof PVAArray)
            return decodeNTArray(actual);
        // TODO: not really sure how to handle arbitrary structures -- no solid use cases yet...

        // Create string that indicates name of unknown type
        return VString.of(actual.format(),
                          Alarm.of(AlarmSeverity.UNDEFINED, AlarmStatus.CLIENT, "Unknown type"),
                          Time.now());
    }

    /** Attempt to decode a scalar {@link VType}
     *  @param struct PVA data for a scalar
     *  @return Value
     *  @throws Exception on error decoding the scalar
     */
    private static VType decodeScalar(final PVAStructure struct) throws Exception
    {
        final PVAData field = struct.get("value");
        if (field instanceof PVANumber)
            return Decoders.decodeNumber(struct, (PVANumber) field);
        if (field instanceof PVAString)
            return Decoders.decodeString(struct, (PVAString) field);
        throw new Exception("Expected struct with scalar 'value', got " + struct);
    }

    /** Decode table from NTTable
     *  @param struct
     *  @return
     *  @throws Exception
     */
    private static VType decodeNTTable(final PVAStructure struct) throws Exception
    {
        final PVAStringArray labels_array = struct.get("labels");
        final List<String> names  = new ArrayList<>(List.of(labels_array.get()));

        final List<Class<?>> types = new ArrayList<>(names.size());
        final List<Object> values = new ArrayList<>(names.size());
        final PVAStructure value_struct = struct.get("value");
        for (PVAData column : value_struct.get())
        {
            if (column instanceof PVADoubleArray)
            {
                final PVADoubleArray typed = (PVADoubleArray)column;
                types.add(Double.TYPE);
                values.add(ArrayDouble.of(typed.get()));
            }
            else if (column instanceof PVAFloatArray)
            {
                final PVAFloatArray typed = (PVAFloatArray)column;
                types.add(Float.TYPE);
                values.add(ArrayFloat.of(typed.get()));
            }
            else if (column instanceof PVAIntArray)
            {
                final PVAIntArray typed = (PVAIntArray)column;
                types.add(Integer.TYPE);
                if (typed.isUnsigned())
                    values.add(ArrayUInteger.of(typed.get()));
                else
                    values.add(ArrayInteger.of(typed.get()));
            }
            else if (column instanceof PVAStringArray)
            {
                final PVAStringArray typed = (PVAStringArray)column;
                types.add(String.class);
                values.add(List.of(typed.get()));
            }
        }

        return VTable.of(types, names, values);
    }

    /** Decode 'value', 'timeStamp', 'alarm' of NTArray
     *  @param struct
     *  @return
     *  @throws Exception
     */
    private static VType decodeNTArray(final PVAStructure struct) throws Exception
    {
        final PVAData field = struct.get("value");
        if (field instanceof PVADoubleArray)
            return Decoders.decodeDoubleArray(struct, (PVADoubleArray) field);
        if (field instanceof PVAFloatArray)
            return Decoders.decodeFloatArray(struct, (PVAFloatArray) field);
        if (field instanceof PVALongArray)
            return Decoders.decodeLongArray(struct, (PVALongArray) field);
        if (field instanceof PVAIntArray)
            return Decoders.decodeIntArray(struct, (PVAIntArray) field);
        if (field instanceof PVAShortArray)
            return Decoders.decodeShortArray(struct, (PVAShortArray) field);
        if (field instanceof PVAByteArray)
            return Decoders.decodeByteArray(struct, (PVAByteArray) field);
        if (field instanceof PVAStringArray)
            return Decoders.decodeStringArray(struct, (PVAStringArray) field);
        return VString.of(struct.format(),
                          Alarm.of(AlarmSeverity.UNDEFINED, AlarmStatus.CLIENT, "Unknown array type"),
                          Time.now());
    }
}
