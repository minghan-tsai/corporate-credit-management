package com.minghan.credit.service;

import com.minghan.credit.dto.CreateDrawdownRequest;
import com.minghan.credit.dto.DrawdownResponse;
import com.minghan.credit.entity.AppUser;
import com.minghan.credit.entity.CreditLimit;
import com.minghan.credit.entity.Drawdown;
import com.minghan.credit.repository.AppUserRepository;
import com.minghan.credit.repository.CreditLimitRepository;
import com.minghan.credit.repository.DrawdownRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class DrawdownService {

    private final AppUserRepository appUserRepository;
    private final CreditLimitRepository creditLimitRepository;
    private final DrawdownRepository drawdownRepository;

    public DrawdownService(
            AppUserRepository appUserRepository,
            CreditLimitRepository creditLimitRepository,
            DrawdownRepository drawdownRepository) {
        this.appUserRepository = appUserRepository;
        this.creditLimitRepository = creditLimitRepository;
        this.drawdownRepository = drawdownRepository;
    }

    // 取得使用者、鎖定額度、驗證、建立動用紀錄及扣減可用額度都在同一個 Transaction。
    // 任一 unchecked exception 或資料庫錯誤都會使 Drawdown insert 與額度更新一起 rollback。
    @Transactional
    public DrawdownResponse create(
            Long creditLimitId,
            CreateDrawdownRequest request) {
        AppUser createdBy = getCurrentUser();

        CreditLimit creditLimit = creditLimitRepository.findByIdForUpdate(creditLimitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Credit limit not found"));

        BigDecimal amount = request.amount();

        // Drawdown Business Rule：金額必須為正數，且不得超過鎖定後讀到的可用額度。
        if (amount == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Drawdown amount is required");
        }

        // BigDecimal 以 compareTo 比較數值，避免 equals 的 scale 差異。
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Drawdown amount must be greater than zero");
        }

        if (amount.compareTo(creditLimit.getAvailableAmount()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Drawdown amount cannot exceed available amount");
        }

        Drawdown drawdown = new Drawdown(creditLimit, amount, createdBy);
        Drawdown savedDrawdown = drawdownRepository.save(drawdown);

        // creditLimit 是本 Transaction 內由鎖定查詢取得的 managed Entity，
        // Dirty Checking 會在 commit 前將扣減結果寫回。
        creditLimit.decreaseAvailableAmount(amount);

        return toResponse(savedDrawdown);
    }

    private DrawdownResponse toResponse(Drawdown drawdown) {
        return new DrawdownResponse(
                drawdown.getId(),
                drawdown.getCreditLimit().getId(),
                drawdown.getAmount(),
                drawdown.getCreatedBy().getId(),
                drawdown.getCreatedAt());
    }

    private AppUser getCurrentUser() {
        // 沿用 Stage 5：JWT filter 將 UserDetails 放入 SecurityContext，再查回 domain AppUser。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Authenticated user is required");
        }

        return appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
