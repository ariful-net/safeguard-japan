package jp.safeguard.service;

import jp.safeguard.exception.ShelterNotFoundException;
import jp.safeguard.mapper.ShelterMapper;
import jp.safeguard.model.dto.ShelterResult;
import jp.safeguard.model.entity.Shelter;
import jp.safeguard.util.GeoUtils;
import jp.safeguard.util.SupportedLang;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 避難所サービス。距離計算・徒歩時間算出・並び替え。
 * 参照のみなので readOnly（詳細設計書 9.4）。
 */
@Service
@Transactional(readOnly = true)
public class ShelterService {

    /** 徒歩速度 分速80m（不動産公正競争規約と同じ基準） */
    public static final double WALK_METERS_PER_MINUTE = 80.0;
    public static final int MAX_RESULTS = 10;

    private final ShelterMapper shelterMapper;

    public ShelterService(ShelterMapper shelterMapper) {
        this.shelterMapper = shelterMapper;
    }

    /**
     * 周辺の避難所を近い順に最大10件返す。
     *
     * @param type earthquake / tsunami / flood / null
     */
    public List<ShelterResult> findNearby(double lat, double lon, int radiusKm, String type, String lang) {
        double dLat = GeoUtils.latDelta(radiusKm);
        double dLon = GeoUtils.lonDelta(radiusKm, lat);
        double radiusM = radiusKm * 1000.0;
        String l = SupportedLang.normalize(lang);

        return shelterMapper.findInBox(lat - dLat, lat + dLat, lon - dLon, lon + dLon, type)
                .stream()
                .map(s -> toResult(s, lat, lon, l))
                .filter(r -> r.distanceM() <= radiusM)
                .sorted(Comparator.comparingInt(ShelterResult::distanceM))
                .limit(MAX_RESULTS)
                .toList();
    }

    /** 詳細モーダル用に1件取得。存在しなければ例外。 */
    public Shelter findById(Long id) {
        Shelter s = shelterMapper.findById(id);
        if (s == null) {
            throw new ShelterNotFoundException(id);
        }
        return s;
    }

    /** 距離(m)から徒歩分数を算出（切り上げ）。800m → 10分。 */
    public static int walkMinutes(double distanceM) {
        return (int) Math.ceil(distanceM / WALK_METERS_PER_MINUTE);
    }

    private ShelterResult toResult(Shelter s, double lat, double lon, String lang) {
        double sLat = s.getLatitude().doubleValue();
        double sLon = s.getLongitude().doubleValue();
        double d = GeoUtils.distanceMeters(lat, lon, sLat, sLon);
        return new ShelterResult(
                s.getId(),
                s.nameFor(lang),
                s.getNameJa(),
                s.getAddress(),
                sLat,
                sLon,
                (int) Math.round(d),
                walkMinutes(d),
                s.getCapacity(),
                Boolean.TRUE.equals(s.getEarthquake()),
                Boolean.TRUE.equals(s.getTsunami()),
                Boolean.TRUE.equals(s.getFlood()));
    }
}
