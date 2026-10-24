package jp.safeguard.service;

import jp.safeguard.mapper.PhraseMapper;
import jp.safeguard.model.dto.PhraseView;
import jp.safeguard.util.SupportedLang;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 防災フレーズサービス。言語別の行動手順・安否定型文・備蓄リストを取得する。
 */
@Service
@Transactional(readOnly = true)
public class PhraseService {

    public static final String SAFETY = "Safety";
    public static final String FOOD = "Food";
    public static final String MEDICAL = "Medical";
    public static final String GENERAL = "General";

    private static final Set<String> DISASTER_TYPES = Set.of("earthquake", "tsunami", "flood");

    private final PhraseMapper phraseMapper;

    public PhraseService(PhraseMapper phraseMapper) {
        this.phraseMapper = phraseMapper;
    }

    public List<PhraseView> findByCategory(String category, String lang) {
        String l = SupportedLang.normalize(lang);
        return phraseMapper.findByCategory(category).stream()
                .map(p -> new PhraseView(p.getId(), p.textFor(l), p.getTextJa()))
                .toList();
    }

    /** 災害種別ごとの行動手順。不正な種別は earthquake 扱い。 */
    public List<PhraseView> actionSteps(String disasterType, String lang) {
        return findByCategory("ActionStep." + normalizeType(disasterType), lang);
    }

    public static String normalizeType(String type) {
        return (type != null && DISASTER_TYPES.contains(type)) ? type : "earthquake";
    }
}
