package core.ms.portfolio.domain.value_object;

import core.ms.portfolio.domain.AssetReservation;
import core.ms.portfolio.domain.CashReservation;
import core.ms.shared.money.Money;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ReservationValidationDSL {

    private final ReservationTransitionContext context;
    private final List<ValidationResult.ValidationError> errors;

    private ReservationValidationDSL(ReservationTransitionContext context) {
        this.context = context;
        this.errors = new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public ReservationValidationDSL withContext(ReservationTransitionContext context) {
            return new ReservationValidationDSL(context);
        }
    }

    // ===== CONFIRMATION VALIDATIONS =====

    public ReservationValidationDSL validateTimeWindow() {
        Instant now = context.getTransitionTime();
        Instant expiration = context.getReservation().getExpirationTime();

        if (now.isAfter(expiration)) {
            errors.add(new ValidationResult.ValidationError(
                    "Reservation has expired", "timeWindow"
            ));
        }
        return this;
    }

    public ReservationValidationDSL validateExecution() {
        var execution = context.getExecutionDetails();
        if (execution == null) {
            errors.add(new ValidationResult.ValidationError(
                    "Execution details required for confirmation", "executionDetails"
            ));
            return this;
        }

        if (execution.getExecutedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationResult.ValidationError(
                    "Executed quantity must be positive", "executedQuantity"
            ));
        }

        BigDecimal reservedQty = context.getReservation().getReservedQuantity();
        if (execution.getExecutedQuantity().compareTo(reservedQty) > 0) {
            errors.add(new ValidationResult.ValidationError(
                    String.format("Executed quantity %s exceeds reserved %s",
                            execution.getExecutedQuantity(), reservedQty),
                    "executedQuantity"
            ));
        }

        return this;
    }

    public ReservationValidationDSL validateResourceAvailability() {
        var reservation = context.getReservation();
        var portfolio = context.getPortfolio();

        if (reservation instanceof CashReservation cashRes) {
            Money reservedAmount = cashRes.getReservedAmount();
            Money availableReserved = portfolio.getReservedCash(reservedAmount.getCurrency());

            if (availableReserved.isLessThan(reservedAmount)) {
                errors.add(new ValidationResult.ValidationError(
                        String.format("Insufficient reserved funds. Required: %s, Available: %s",
                                reservedAmount.toDisplayString(), availableReserved.toDisplayString()),
                        "resourceAvailability"
                ));
            }
        } else if (reservation instanceof AssetReservation assetRes) {
            BigDecimal requiredQty = assetRes.getQuantity();
            BigDecimal availableQty = portfolio.getReservedAssets(assetRes.getSymbol());

            if (availableQty.compareTo(requiredQty) < 0) {
                errors.add(new ValidationResult.ValidationError(
                        String.format("Insufficient reserved assets. Required: %s, Available: %s",
                                requiredQty, availableQty),
                        "resourceAvailability"
                ));
            }
        }

        return this;
    }

    public ReservationValidationDSL validateNoDoubleSpending() {
        var reservation = context.getReservation();
        var portfolio = context.getPortfolio();

        if (!reservation.getState().canConfirm()) {
            errors.add(new ValidationResult.ValidationError(
                    "Reservation cannot be confirmed in current state", "state"
            ));
        }

        if (portfolio.hasConsumedReservation(reservation.getReservationId())) {
            errors.add(new ValidationResult.ValidationError(
                    "Reservation has already been consumed", "doubleSpending"
            ));
        }

        return this;
    }

    // ===== RELEASE VALIDATIONS =====

    public ReservationValidationDSL validateResourcesFullyReserved() {
        var reservation = context.getReservation();

        if (!reservation.getConsumedAmount().equals(BigDecimal.ZERO)) {
            errors.add(new ValidationResult.ValidationError(
                    "Cannot release partially consumed reservation", "consumedAmount"
            ));
        }

        return this;
    }

    public ReservationValidationDSL validateReleaseAuthorization() {
        var releaseContext = context.getReleaseContext();

        if (releaseContext == null) {
            errors.add(new ValidationResult.ValidationError(
                    "Release context required", "releaseContext"
            ));
            return this;
        }

        if (!releaseContext.hasValidReason()) {
            errors.add(new ValidationResult.ValidationError(
                    "Release reason required", "releaseReason"
            ));
        }

        if (!releaseContext.isSystemTriggered() && !releaseContext.isAuthorized()) {
            errors.add(new ValidationResult.ValidationError(
                    "Release not authorized", "authorization"
            ));
        }

        return this;
    }

    // ===== EXPIRATION VALIDATIONS =====

    public ReservationValidationDSL validateExpirationTime() {
        var reservation = context.getReservation();
        Instant now = context.getTransitionTime();
        Instant expiration = reservation.getExpirationTime();

        if (now.isBefore(expiration)) {
            errors.add(new ValidationResult.ValidationError(
                    String.format("Reservation not yet expired. Expires at: %s", expiration),
                    "expirationTime"
            ));
        }

        return this;
    }

    public ReservationValidationDSL validateNoRecentActivity() {
        var reservation = context.getReservation();
        Duration gracePeriod = Duration.ofSeconds(30);
        Instant lastActivity = reservation.getLastActivityTime();
        Duration timeSinceActivity = Duration.between(lastActivity, context.getTransitionTime());

        if (timeSinceActivity.compareTo(gracePeriod) <= 0) {
            errors.add(new ValidationResult.ValidationError(
                    "Cannot expire reservation with recent activity", "recentActivity"
            ));
        }

        return this;
    }

    // ===== BUILD RESULT =====

    public ValidationResult build() {
        if (errors.isEmpty()) {
            return ValidationResult.valid();
        }
        return ValidationResult.invalid(errors);
    }
}