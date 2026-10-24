package jp.safeguard.model.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 避難所検索条件（詳細設計書 7章）。
 * メッセージは messages_*.properties のキーで多言語化する。
 */
public class ShelterSearchForm {

    public static final int DEFAULT_RADIUS_KM = 3;

    @NotNull(message = "{validation.location}")
    @DecimalMin(value = "-90.0", message = "{validation.location}")
    @DecimalMax(value = "90.0", message = "{validation.location}")
    private Double lat;

    @NotNull(message = "{validation.location}")
    @DecimalMin(value = "-180.0", message = "{validation.location}")
    @DecimalMax(value = "180.0", message = "{validation.location}")
    private Double lon;

    @Min(value = 1, message = "{validation.radius}")
    @Max(value = 20, message = "{validation.radius}")
    private Integer radius = DEFAULT_RADIUS_KM;

    /** 任意。空なら全種別。 */
    @Pattern(regexp = "^(earthquake|tsunami|flood)?$", message = "{validation.type}")
    private String type;

    /** 任意。不正値は ja に置換するので検証しない。 */
    private String lang;

    public int radiusOrDefault() {
        return radius == null ? DEFAULT_RADIUS_KM : radius;
    }

    public String typeOrNull() {
        return (type == null || type.isBlank()) ? null : type;
    }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }
    public Integer getRadius() { return radius; }
    public void setRadius(Integer radius) { this.radius = radius; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
}
