package jp.safeguard.exception;

/**
 * 指定 ID の避難所が存在しない場合の例外。
 */
public class ShelterNotFoundException extends RuntimeException {

    public ShelterNotFoundException(Long id) {
        super("Shelter not found: id=" + id);
    }
}
