package jp.safeguard.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jp.safeguard.model.form.ShelterSearchForm;
import jp.safeguard.util.SupportedLang;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 入力検証の境界値テスト（詳細設計書 7章・11章）。
 */
class ShelterSearchFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static ShelterSearchForm form(Double lat, Double lon, Integer radius, String type) {
        ShelterSearchForm f = new ShelterSearchForm();
        f.setLat(lat);
        f.setLon(lon);
        f.setRadius(radius);
        f.setType(type);
        return f;
    }

    @ParameterizedTest(name = "OK: lat={0}, lon={1}, radius={2}")
    @CsvSource({
            "90.0, 180.0, 1",
            "-90.0, -180.0, 20",
            "34.70, 135.49, 3"
    })
    void 境界値は通る(double lat, double lon, int radius) {
        assertThat(validator.validate(form(lat, lon, radius, "earthquake"))).isEmpty();
    }

    @ParameterizedTest(name = "NG: lat={0}, lon={1}, radius={2}")
    @CsvSource({
            "90.1, 135.0, 3",
            "-90.1, 135.0, 3",
            "34.0, 180.1, 3",
            "34.0, -180.1, 3",
            "34.0, 135.0, 0",
            "34.0, 135.0, 21"
    })
    void 範囲外はエラー(double lat, double lon, int radius) {
        Set<ConstraintViolation<ShelterSearchForm>> v = validator.validate(form(lat, lon, radius, null));
        assertThat(v).isNotEmpty();
    }

    @Test
    void 緯度経度は必須() {
        assertThat(validator.validate(form(null, null, 3, null))).hasSize(2);
    }

    @Test
    void 半径未指定は初期値3() {
        ShelterSearchForm f = new ShelterSearchForm();
        assertThat(f.radiusOrDefault()).isEqualTo(3);
    }

    @Test
    void 列挙外の災害種別はエラー() {
        assertThat(validator.validate(form(34.0, 135.0, 3, "fire"))).hasSize(1);
        assertThat(validator.validate(form(34.0, 135.0, 3, ""))).isEmpty();
    }

    @Test
    void 不正な言語コードはjaに置換() {
        assertThat(SupportedLang.normalize("xx")).isEqualTo("ja");
        assertThat(SupportedLang.normalize(null)).isEqualTo("ja");
        assertThat(SupportedLang.normalize("en-US")).isEqualTo("en");
        assertThat(SupportedLang.normalize("BN")).isEqualTo("bn");
    }
}
