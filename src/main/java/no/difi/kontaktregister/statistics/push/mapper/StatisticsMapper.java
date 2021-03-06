package no.difi.kontaktregister.statistics.push.mapper;

import no.difi.kontaktregister.statistics.fetch.consumer.KontaktregisterField;
import no.difi.kontaktregister.statistics.fetch.consumer.KontaktregisterValue;
import no.difi.kontaktregister.statistics.util.NameTranslateDefinitions;
import no.difi.statistics.ingest.client.model.TimeSeriesPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.difi.kontaktregister.statistics.util.NameTranslateDefinitions.*;
import static no.difi.statistics.ingest.client.model.TimeSeriesPoint.timeSeriesPoint;

@Component
public class StatisticsMapper {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public List<TimeSeriesPoint> map(List<KontaktregisterField> fields, ZonedDateTime fromDateTime) {
        return mapMeasurements(fields, fromDateTime);
    }

    private List<TimeSeriesPoint> mapMeasurements(List<KontaktregisterField> fields, ZonedDateTime dateTime) {
        Map<NameTranslateDefinitions, List<Long>> measurements = toHashMap(fields);
        return mapBulk(measurements, dateTime);
    }

    private List<TimeSeriesPoint> mapBulk(Map<NameTranslateDefinitions, List<Long>> measurements, ZonedDateTime dateTime) {
        List<TimeSeriesPoint> tsp = new ArrayList<>();
        for (int i = 0; i < measurements.get(D5_1).size(); i++) {
            tsp.add(
                    timeSeriesPoint()
                            .timestamp(dateTime.plusHours(i))
                            .measurements(getMeasurementForIndex(measurements, i))
                            .build()
            );
        }
        return tsp;
    }

    private Map<String, Long> getMeasurementForIndex(Map<NameTranslateDefinitions, List<Long>> measurementList, int index) {
        Map<String, Long> measurements = Stream.of(D5_1, D5_2, D5_4, D5_5, D5_6, D5_7, D7_3, D7_4)
                .filter(measurementList::containsKey)
                .collect(toMap(NameTranslateDefinitions::getStatisticId, f -> measurementList.get(f).get(index)));
        long brukereMedReservasjon = measurementList.get(D5_5).get(index) + measurementList.get(D5_6).get(index);
        measurements.put(D5_5_6.getStatisticId(), brukereMedReservasjon);
        long brukereMedDigipost = 0;
        if(measurementList.get(D7_3) != null && measurementList.get(D7_3).get(index) != null){
            brukereMedDigipost = measurementList.get(D7_3).get(index);
        }else{
            logger.error("Failed to read from Digipost fields, set to 0.");
        }
        long brukereMedEboks = 0;
        if(hasEboksUsers(measurementList, index)){
            brukereMedEboks = measurementList.get(D7_4).get(index);
        }
        if(hasEboksUsersWithUtdatedOrgnr(measurementList, index)) {
            brukereMedEboks += measurementList.get(D7_4_OLD).get(index); // might be both old and new eboks orgnr inside same day/month when the orgnr switch was done. These must be added.
        }
        if (!hasEboksUsers(measurementList, index) && !hasEboksUsersWithUtdatedOrgnr(measurementList, index)) {
            logger.error("Failed to read from eBoks fields, set to 0.");
        }
        long brukereMedPostkasse = brukereMedDigipost + brukereMedEboks;
        measurements.put(D7_3_4.getStatisticId(), brukereMedPostkasse);
        return measurements;
    }

    private boolean hasEboksUsersWithUtdatedOrgnr(Map<NameTranslateDefinitions, List<Long>> measurementList, int index) {
        return measurementList.get(D7_4_OLD) != null && measurementList.get(D7_4_OLD).get(index) != null;
    }

    private boolean hasEboksUsers(Map<NameTranslateDefinitions, List<Long>> measurementList, int index) {
        return measurementList.get(D7_4) != null && measurementList.get(D7_4).get(index) != null;
    }

    private Map<NameTranslateDefinitions, List<Long>> toHashMap(List<KontaktregisterField> fields) {
        Map<NameTranslateDefinitions, List<Long>> measurements = new HashMap<>();

        for (KontaktregisterField field : fields) {
            if (findD5Id(field) != null) {
                measurements.put(findD5Id(field), toLongList(field.getValues().subList(1, field.getValues().size())));
            } else if (findD7Id(field) != null) {
                measurements.put(findD7Id(field), toLongList(field.getValues().subList(3, field.getValues().size())));
            }
        }

        return measurements;
    }

    private List<Long> toLongList(List<KontaktregisterValue> values) {
        return values.stream()
                .map(e -> Long.valueOf(e.getValue()))
                .collect(toList());
    }

    private NameTranslateDefinitions findD5Id(KontaktregisterField field) {
        return NameTranslateDefinitions.find(field.getValues().get(0).getValue());
    }

    private NameTranslateDefinitions findD7Id(KontaktregisterField field) {
        StringJoiner fieldId = new StringJoiner("");
        if (field.getValues().size() >= 4) {
            for (int i = 0; i < 3; i++) {
                fieldId.add(field.getValues().get(i).getValue());
            }
        }
        return NameTranslateDefinitions.find(fieldId.toString());
    }

}
