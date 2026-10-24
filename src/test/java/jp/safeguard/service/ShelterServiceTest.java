package jp.safeguard.service;

import jp.safeguard.exception.ShelterNotFoundException;
import jp.safeguard.mapper.ShelterMapper;
import jp.safeguard.model.dto.ShelterResult;
import jp.safeguard.model.entity.Shelter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 単体テスト観点（詳細設計書 11章）: 正常系・境界値・異常系。
 */
class ShelterServiceTest {

    // 基準点: 大阪駅
    private static final double LAT = 34.7025;
    private static final double LON = 135.4959;

    private final ShelterMapper mapper = mock(ShelterMapper.class);
    private final ShelterService service = new ShelterService(mapper);

    private static Shelter shelter(long id, String name, double lat, double lon) {
        return new Shelter(id, name, name + " EN", "住所", BigDecimal.valueOf(lat), BigDecimal.valueOf(lon),
                true, false, false, 100);
    }

    @Test
    void 距離の近い順に返す() {
        when(mapper.findInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any())).thenReturn(List.of(
                shelter(1, "遠い", LAT + 0.02, LON),   // 約2.2km
                shelter(2, "近い", LAT + 0.005, LON),  // 約0.56km
                shelter(3, "中間", LAT + 0.01, LON))); // 約1.1km

        List<ShelterResult> result = service.findNearby(LAT, LON, 3, null, "ja");

        assertThat(result).extracting(ShelterResult::id).containsExactly(2L, 3L, 1L);
        assertThat(result.get(0).name()).isEqualTo("近い");
    }

    @Test
    void 徒歩時間は分速80mで切り上げ_800mは10分() {
        assertThat(ShelterService.walkMinutes(800)).isEqualTo(10);
        assertThat(ShelterService.walkMinutes(801)).isEqualTo(11);
        assertThat(ShelterService.walkMinutes(1)).isEqualTo(1);
    }

    @Test
    void 半径外の避難所は除外する() {
        when(mapper.findInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any())).thenReturn(List.of(
                shelter(1, "四角の角", LAT + 0.008, LON + 0.0095))); // 四角形内だが約1.25km

        assertThat(service.findNearby(LAT, LON, 1, null, "ja")).isEmpty();
    }

    @Test
    void 最大10件に絞る() {
        List<Shelter> many = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            many.add(shelter(i, "S" + i, LAT + i * 0.001, LON));
        }
        when(mapper.findInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any())).thenReturn(many);

        assertThat(service.findNearby(LAT, LON, 20, null, "ja")).hasSize(10);
    }

    @Test
    void 結果0件なら空リスト() {
        when(mapper.findInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any())).thenReturn(List.of());

        assertThat(service.findNearby(LAT, LON, 3, "flood", "en")).isEmpty();
    }

    @Test
    void 英語では英語名_無ければ日本語名() {
        Shelter noEn = shelter(1, "名前", LAT, LON);
        noEn.setNameEn(null);
        when(mapper.findInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(shelter(2, "公園", LAT, LON), noEn));

        List<ShelterResult> result = service.findNearby(LAT, LON, 3, null, "en");

        assertThat(result).extracting(ShelterResult::name).containsExactlyInAnyOrder("公園 EN", "名前");
    }

    @Test
    void 存在しないIDは例外() {
        when(mapper.findById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ShelterNotFoundException.class);
    }
}
