package jp.safeguard.model.entity;

/**
 * フレーズエンティティ（phrases 1行分）。
 * category: ActionStep.earthquake / ActionStep.tsunami / ActionStep.flood /
 *           Safety / Medical / Food / General
 */
public class Phrase {

    private Long id;
    private String category;
    private String textJa;
    private String textEn;
    private String textBn;
    private String textVi;
    private Integer sortOrder;

    public Phrase() {
    }

    public Phrase(Long id, String category, String textJa, String textEn,
                  String textBn, String textVi, Integer sortOrder) {
        this.id = id;
        this.category = category;
        this.textJa = textJa;
        this.textEn = textEn;
        this.textBn = textBn;
        this.textVi = textVi;
        this.sortOrder = sortOrder;
    }

    /** 言語別の文言。空なら日本語にフォールバック。 */
    public String textFor(String lang) {
        String t = switch (lang) {
            case "en" -> textEn;
            case "bn" -> textBn;
            case "vi" -> textVi;
            default -> textJa;
        };
        return (t == null || t.isBlank()) ? textJa : t;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTextJa() { return textJa; }
    public void setTextJa(String textJa) { this.textJa = textJa; }
    public String getTextEn() { return textEn; }
    public void setTextEn(String textEn) { this.textEn = textEn; }
    public String getTextBn() { return textBn; }
    public void setTextBn(String textBn) { this.textBn = textBn; }
    public String getTextVi() { return textVi; }
    public void setTextVi(String textVi) { this.textVi = textVi; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
