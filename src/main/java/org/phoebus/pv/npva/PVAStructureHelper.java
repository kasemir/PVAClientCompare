/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.npva;

import java.util.List;

import org.epics.pva.data.PVAByte;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAFloat;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVALong;
import org.epics.pva.data.PVANumber;
import org.epics.pva.data.PVAShort;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStringArray;
import org.epics.pva.data.PVAStructure;
import org.epics.vtype.VType;

@SuppressWarnings("nls")
public class PVAStructureHelper
{
    public static VType getVType(final PVAStructure struct) throws Exception
    {
        // Handle normative types
        String type = struct.getStructureName();
        if (type.startsWith("epics:nt/"))
            type = type.substring(9);
        if (type.equals("NTScalar:1.0"))
            return decodeScalar(struct);
        if (type.equals("NTEnum:1.0"))
            return Decoders.decodeEnum(struct);
//        if (type.equals("NTScalarArray:1.0"))
//            return decodeNTArray(data);
//        if (type.equals("NTNDArray:1.0"))
//            return ImageDecoder.decode(data);
//        if (type.equals("NTTable:1.0"))
//            return decodeNTTable(data);

        // TODO Auto-generated method stub
        return null;
    }

    /** Attempt to decode a scalar {@link VType}
     *  @param struct PVA data for a scalar
     *  @return Value
     *  @throws Exception on error decoding the scalar
     */
    private static VType decodeScalar(final PVAStructure struct) throws Exception
    {
        final PVAData field = struct.get("value");
        if (field == null)
            throw new Exception("Expected struct with scalar 'value', got " + struct);
        if (field instanceof PVADouble)
            return Decoders.decodeDouble(struct, (PVADouble) field);
        if (field instanceof PVAFloat)
            return Decoders.decodeFloat(struct, (PVAFloat) field);
        if (field instanceof PVALong)
            return Decoders.decodeLong(struct, (PVALong) field);
        if (field instanceof PVAInt)
            return Decoders.decodeInt(struct, (PVAInt) field);
        if (field instanceof PVAShort)
            return Decoders.decodeShort(struct, (PVAShort) field);
        if (field instanceof PVAByte)
            return Decoders.decodeByte(struct, (PVAByte) field);
        if (field instanceof PVAString)
            return Decoders.decodeString(struct, (PVAString) field);
        throw new Exception("Cannot handle " + field.getClass().getName());
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
