package jp.safeguard.mapper;

import jp.safeguard.model.entity.Phrase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * phrases テーブルの検索SQL（SQL本体は resources/mapper/PhraseMapper.xml）。
 */
@Mapper
public interface PhraseMapper {

    /** カテゴリのフレーズを表示順（昇順）で取得する。 */
    List<Phrase> findByCategory(@Param("category") String category);
}
