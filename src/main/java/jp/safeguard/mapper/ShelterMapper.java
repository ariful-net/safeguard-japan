package jp.safeguard.mapper;

import jp.safeguard.model.entity.Shelter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * shelters テーブルの検索SQL（SQL本体は resources/mapper/ShelterMapper.xml）。
 */
@Mapper
public interface ShelterMapper {

    /**
     * 緯度経度の範囲（四角形）で候補を取得する。
     *
     * @param type earthquake / tsunami / flood / null（全種別）
     */
    List<Shelter> findInBox(@Param("minLat") double minLat,
                            @Param("maxLat") double maxLat,
                            @Param("minLon") double minLon,
                            @Param("maxLon") double maxLon,
                            @Param("type") String type);

    Shelter findById(@Param("id") Long id);
}
