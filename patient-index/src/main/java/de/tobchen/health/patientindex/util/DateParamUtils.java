package de.tobchen.health.patientindex.util;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.lang.Nullable;

import ca.uhn.fhir.rest.param.DateParam;

public class DateParamUtils
{
    public static @Nullable Instant completeToHighestDate(@Nullable DateParam dateParam)
    {
        Instant instant;

        if (dateParam != null)
        {
            var inputDate = dateParam.getValue();
            var inputInstant = inputDate.toInstant();

            var precision = dateParam.getPrecision();
            switch (precision)
            {
            case YEAR:
                instant = ZonedDateTime.ofInstant(inputInstant, ZoneId.systemDefault())
                    .with(TemporalAdjusters.lastDayOfYear()).with(LocalTime.MAX).toInstant();
                break;
            case MONTH:
                instant = ZonedDateTime.ofInstant(inputInstant, ZoneId.systemDefault())
                    .with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX).toInstant();
                break;
            case DAY:
                instant = ZonedDateTime.ofInstant(inputInstant, ZoneId.systemDefault())
                    .with(LocalTime.MAX).toInstant();
                break;
            case MINUTE:
                instant = DateUtils.setMilliseconds(DateUtils.setSeconds(inputDate, 59), 999).toInstant();
                break;
            case SECOND:
                instant = DateUtils.setMilliseconds(inputDate, 999).toInstant();
                break;
            case MILLI:
            default:
                instant = inputInstant;
                break;
            }
        }
        else
        {
            instant = null;
        }

        return instant;
    }
}
