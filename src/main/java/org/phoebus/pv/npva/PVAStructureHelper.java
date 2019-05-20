/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.npva;

import java.util.List;

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
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
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
//        if (type.equals("NTTable:1.0"))
//            return decodeNTTable(data);

        // TODO Auto-generated method stub
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

    public static double getDoubleValue(final PVAStructure struct, String name,
                                        final double default_value)
    {
        final PVANumber field = struct.get(name);
        if (field != null)
            return field.getNumber().doubleValue();
        else
            return default_value;
    }

    /** @param structure {@link PVAStructure} from which to read
     *  @param name Name of a field in that structure
     *  @return Array of strings
     *  @throws Exception on error
     */
    public static List<String> getStrings(final PVAStructure structure, final String name)
    {
        final PVAStringArray choices = structure.get(name);
        return List.of(choices.get());
    }
}
