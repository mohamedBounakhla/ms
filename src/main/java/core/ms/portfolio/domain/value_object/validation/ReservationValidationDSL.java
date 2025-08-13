package core.ms.portfolio.domain.value_object.validation;

import core.ms.portfolio.domain.entities.AssetReservation;
import core.ms.portfolio.domain.entities.CashReservation;
import core.ms.portfolio.domain.value_object.state.ReservationTransitionContext;
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

    // ===== EXECUTION VALIDATIONS =====

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

    public ReservationValidationDSL validateNoDoubleExecution() {
        var reservation = context.getReservation();
        var portfolio = context.getPortfolio();

        if (!reservation.getState().canExecute()) {
            errors.add(new ValidationResult.ValidationError(
                    "Reservation cannot be executed in current state", "state"
            ));
        }

        if (portfolio.hasExecutedReservation(reservation.getReservationId())) {
            errors.add(new ValidationResult.ValidationError(
                    "Reservation has already been executed", "doubleExecution"
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
            BigDecimal requiredQty = assetRes.getReservedQuantity();
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

    // ===== CANCELLATION VALIDATIONS =====

    public ReservationValidationDSL validateResourcesFullyReserved() {
        var reservation = context.getReservation();

        if (!reservation.getConsumedAmount().equals(BigDecimal.ZERO)) {
            errors.add(new ValidationResult.ValidationError(
                    "Cannot cancel partially consumed reservation", "consumedAmount"
            ));
        }

        return this;
    }

    public ReservationValidationDSL validateCancellationAuthorization() {
        var cancellationContext = context.getCancellationContext();

        if (cancellationContext == null) {
            errors.add(new ValidationResult.ValidationError(
                    "Cancellation context required", "cancellationContext"
            ));
            return this;
        }

        if (!cancellationContext.hasValidReason()) {
            errors.add(new ValidationResult.ValidationError(
                    "Cancellation reason required", "cancellationReason"
            ));
        }

        if (!cancellationContext.isSystemTriggered() && !cancellationContext.isAuthorized()) {
            errors.add(new ValidationResult.ValidationError(
                    "Cancellation not authorized", "authorization"
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