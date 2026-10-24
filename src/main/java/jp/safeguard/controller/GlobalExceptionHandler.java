package jp.safeguard.controller;

import jakarta.servlet.http.HttpServletRequest;
import jp.safeguard.exception.ShelterNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * 共通エラー制御（詳細設計書 9.4）。
 * DB障害時は共通エラー画面（API は JSON）。ログに緯度経度・クエリ文字列は残さない。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ShelterNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(ShelterNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
    }

    @ExceptionHandler(DataAccessException.class)
    public Object dataAccess(DataAccessException e, HttpServletRequest request) {
        // request.getRequestURI() にはクエリ文字列（lat/lon）が含まれない
        log.error("DB error on {}: {}", request.getRequestURI(), e.getClass().getSimpleName());
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "db_unavailable"));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
        mav.addObject("status", 503);
        return mav;
    }
}
