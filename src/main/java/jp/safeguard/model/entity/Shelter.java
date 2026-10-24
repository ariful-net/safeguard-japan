package jp.safeguard.model.entity;

import java.math.BigDecimal;

/**
 * 避難所エンティティ（shelters 1行分）。
 */
public class Shelter {

    private Long id;
    private String nameJa;
    private String nameEn;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean earthquake;
    private Boolean tsunami;
    private Boolean flood;
    private Integer capacity;

    public Shelter() {
    }

    public Shelter(Long id, String nameJa, String nameEn, String address,
                   BigDecimal latitude, BigDecimal longitude,
                   Boolean earthquake, Boolean tsunami, Boolean flood, Integer capacity) {
        this.id = id;
        this.nameJa = nameJa;
        this.nameEn = nameEn;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.earthquake = earthquake;
        this.tsunami = tsunami;
        this.flood = flood;
        this.capacity = capacity;
    }

    /** 言語に応じた避難所名。英語名が無ければ日本語名を返す。 */
    public String nameFor(String lang) {
        if ("ja".equals(lang) || nameEn == null || nameEn.isBlank()) {
            return nameJa;
        }
        return nameEn;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNameJa() { return nameJa; }
    public void setNameJa(String nameJa) { this.nameJa = nameJa; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public Boolean getEarthquake() { return earthquake; }
    public void setEarthquake(Boolean earthquake) { this.earthquake = earthquake; }
    public Boolean getTsunami() { return tsunami; }
    public void setTsunami(Boolean tsunami) { this.tsunami = tsunami; }
    public Boolean getFlood() { return flood; }
    public void setFlood(Boolean flood) { this.flood = flood; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}
