/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.npva;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVALong;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;

/** Decodes {@link Time}, {@link Alarm}, {@link Display}, ...
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Decoders
{
    private static final Instant NO_TIME = Instant.ofEpochSecond(0, 0);
    private static final Integer NO_USERTAG = Integer.valueOf(0);

    private static final Display noDisplay = Display.none();

    /** Cache for formats */
    private static final Map<String, NumberFormat> formatterCache =
            new ConcurrentHashMap<>();


    public static VType decodeDouble(final PVAStructure struct, final PVADouble field)
    {
        return VDouble.of(field.get(),
                          decodeAlarm(struct),
                          decodeTime(struct),
                          decodeDisplay(struct));
    }

    private static Alarm decodeAlarm(final PVAStructure struct)
    {
        // Decode alarm_t alarm
        final AlarmSeverity severity;
        final AlarmStatus status;
        final String message;

        final PVAStructure alarm = struct.get("alarm");
        if (alarm != null)
        {
            PVAInt code = alarm.get("severity");
            severity = code == null
                     ? AlarmSeverity.UNDEFINED
                     : AlarmSeverity.values()[code.get()];

            code = alarm.get("status");
            status = code == null
                    ? AlarmStatus.UNDEFINED
                    : AlarmStatus.values()[code.get()];

            final PVAString msg = alarm.get("message");
            message = msg == null ? "<null>" : msg.get();
        }
        else
        {
            severity = AlarmSeverity.NONE;
            status = AlarmStatus.NONE;
            message = AlarmStatus.NONE.name();
        }
        return Alarm.of(severity, status, message);
    }

    private static Time decodeTime(final PVAStructure struct)
    {
        // Decode time_t timeStamp
        final Instant timestamp;
        final Integer usertag;

        final PVAStructure time = struct.get("timeStamp");
        if (time != null)
        {
            final PVALong sec = time.get("secondsPastEpoch");
            final PVAInt nano = time.get("nanoseconds");
            if (sec == null || nano == null)
                timestamp = NO_TIME;
            else
                timestamp = Instant.ofEpochSecond(sec.get(), nano.get());
            final PVAInt user = time.get("userTag");
            usertag = user == null ? NO_USERTAG : user.get();
        }
        else
        {
            timestamp = NO_TIME;
            usertag = NO_USERTAG;
        }
        return Time.of(timestamp, usertag, timestamp.getEpochSecond() > 0);
    }

    /** @param printfFormat Format from NTScalar display.format
     *  @return Suitable NumberFormat
     */
    private static NumberFormat createNumberFormat(final String printfFormat)
    {
        if (printfFormat == null ||
            printfFormat.trim().isEmpty() ||
            printfFormat.equals("%s"))
            return noDisplay.getFormat();
        else
        {
            NumberFormat formatter = formatterCache.get(printfFormat);
            if (formatter != null)
                return formatter;
            formatter = new PrintfFormat(printfFormat);
            formatterCache.put(printfFormat, formatter);
            return formatter;
        }
    }

    static class PrintfFormat extends java.text.NumberFormat
    {
        private static final long serialVersionUID = 1L;
        private final String format;
        public PrintfFormat(final String printfFormat)
        {
            // probe format
            boolean allOK = true;
            try {
                String.format(printfFormat, 0.0);
            } catch (Throwable th) {
                allOK = false;
            }
            // accept it if all is OK
            this.format = allOK ? printfFormat : null;
        }

        private final String internalFormat(double number)
        {
            if (format != null)
                return String.format(format, number);
            else
                return String.valueOf(number);
        }

        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
        {
            toAppendTo.append(internalFormat(number));
            return toAppendTo;
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos)
        {
            toAppendTo.append(internalFormat(number));
            return toAppendTo;
        }

        @Override
        public Number parse(String source, ParsePosition parsePosition)
        {
            throw new UnsupportedOperationException("No parsing.");
        }
    };

    private static Display decodeDisplay(final PVAStructure struct)
    {
        String units;
        NumberFormat format;
        Range display, control, alarm, warn;

        // Decode display_t display
        PVAStructure section = struct.get("display");
        if (section != null)
        {
            PVAString str = section.get("units");
            units = str == null ? noDisplay.getUnit() : str.get();

            // Since EPICS Base 7.0.2.2, qsrv supports 'precision' and 'form'
            final PVAInt prec = section.get("precision");
            if (prec != null)
            {
                final PVAStructure form = section.get("form");
                if (form != null)
                {
                    final PVAInt pv_idx = form.get("index");
                    final int idx = pv_idx == null ? 0 : pv_idx.get();
                    // idx = ["Default", "String", "Binary", "Decimal", "Hex", "Exponential", "Engineering"]
                    // XXX VType doesn't offer a good way to pass the 'form' options on.
                    //     This format is later mostly ignored, only precision is recovered.
                    switch (idx)
                    {
                    case 4:
                        format = createNumberFormat("0x%X");
                        break;
                    case 5:
                    case 6:
                        format = createNumberFormat("%." + prec.get() + "E");
                        break;
                    default:
                        format = NumberFormats.precisionFormat(prec.get());
                    }
                }
                else
                    format = NumberFormats.precisionFormat(prec.get());
            }
            else
            {
                // Earlier PV servers sent 'format' string
                str = section.get("format");
                format = str == null
                    ? noDisplay.getFormat()
                    : createNumberFormat(str.get());
            }

            display = Range.of(PVAStructureHelper.getDoubleValue(section, "limitLow", Double.NaN),
                               PVAStructureHelper.getDoubleValue(section, "limitHigh", Double.NaN));
        }
        else
        {
            units = noDisplay.getUnit();
            format = noDisplay.getFormat();
            display = Range.undefined();
        }

        // Decode control_t control
        section = struct.get("control");
        if (section != null)
            control = Range.of(PVAStructureHelper.getDoubleValue(section, "limitLow", Double.NaN),
                               PVAStructureHelper.getDoubleValue(section, "limitHigh", Double.NaN));
        else
            control = Range.undefined();

        // Decode valueAlarm_t valueAlarm
        section = struct.get("valueAlarm");
        if (section != null)
        {
            alarm = Range.of(PVAStructureHelper.getDoubleValue(section, "lowAlarmLimit", Double.NaN),
                             PVAStructureHelper.getDoubleValue(section, "highAlarmLimit", Double.NaN));
            warn = Range.of(PVAStructureHelper.getDoubleValue(section, "lowWarningLimit", Double.NaN),
                            PVAStructureHelper.getDoubleValue(section, "highWarningLimit", Double.NaN));
        }
        else
            alarm = warn = Range.undefined();

        return Display.of(display, alarm, warn, control, units, format);
    }
}