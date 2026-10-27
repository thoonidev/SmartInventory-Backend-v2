package com.thoonidev.InventoryMgtSystem.specification;

import com.thoonidev.InventoryMgtSystem.enums.TransactionStatus;
import com.thoonidev.InventoryMgtSystem.enums.TransactionType;
import com.thoonidev.InventoryMgtSystem.models.Transaction;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TransactionFilter {

    private static final Map<String, TransactionStatus> STATUS_TRANSLATIONS;
    private static final Map<String, TransactionType> TYPE_TRANSLATIONS;

    static {
        STATUS_TRANSLATIONS = Map.of(
                normalize("pendiente"), TransactionStatus.PENDING,
                normalize("proceso"), TransactionStatus.PROCESSING,
                normalize("completado"), TransactionStatus.COMPLETED,
                normalize("cancelado"), TransactionStatus.CANCELLED
        );

        TYPE_TRANSLATIONS = Map.of(
                normalize("compra"), TransactionType.PURCHASE,
                normalize("venta"), TransactionType.SALE,
                normalize("devolución"), TransactionType.RETURN_TO_SUPPLIER
        );
    }

    // Normalize input by removing accents and converting to lowercase
    public static String normalize(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
    }

    // Dynamic search filter based on search value
    public static Specification<Transaction> byFilter(String searchValue) {
        return (root, query, criteriaBuilder) -> {
            if (searchValue == null || searchValue.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // No filtering
            }

            String trimmedSearch = searchValue.trim();
            Optional<Long> optionalId = parseLong(trimmedSearch);

            // If the search is a valid numeric ID, filter exclusively by ID
            if (optionalId.isPresent()) {
                return criteriaBuilder.equal(root.get("id"), optionalId.get());
            }

            // If not ID, perform text-based search
            String normalizedSearch = normalize(trimmedSearch);
            String searchPattern = "%" + normalizedSearch + "%";

            List<Predicate> predicates = new ArrayList<>();

            // Match translated transaction status
            TransactionStatus statusMatch = STATUS_TRANSLATIONS.get(normalizedSearch);
            if (statusMatch != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), statusMatch));
            }

            // Match translated transaction type
            TransactionType typeMatch = TYPE_TRANSLATIONS.get(normalizedSearch);
            if (typeMatch != null) {
                predicates.add(criteriaBuilder.equal(root.get("transactionType"), typeMatch));
            }

            // Search in basic string fields
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("note")), searchPattern));

            // Search in related user fields
            Join<Transaction, ?> userJoin = root.join("user", JoinType.LEFT);
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("name")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("email")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("phoneNumber")), searchPattern));

            // Search in related supplier fields
            Join<Transaction, ?> supplierJoin = root.join("supplier", JoinType.LEFT);
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(supplierJoin.get("name")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(supplierJoin.get("contactInfo")), searchPattern));

            // Search in related product fields
            Join<Transaction, ?> productJoin = root.join("product", JoinType.LEFT);
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(productJoin.get("name")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(productJoin.get("sku")), searchPattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(productJoin.get("description")), searchPattern));

            // Search in related category (inside product)
            Join<?, ?> categoryJoin = productJoin.join("category", JoinType.LEFT);
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(categoryJoin.get("name")), searchPattern));

            // Combine all conditions with OR to match any field
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }

    // Safely parse a Long value from string
    private static Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    // Filter transactions by month and year using SQL functions
    public static Specification<Transaction> byMonthAndYear(int month, int year) {
        return (root, query, criteriaBuilder) -> {
            Expression<Integer> monthExpression = criteriaBuilder.function("month", Integer.class, root.get("createdAt"));
            Expression<Integer> yearExpression = criteriaBuilder.function("year", Integer.class, root.get("createdAt"));
            Predicate monthPredicate = criteriaBuilder.equal(monthExpression, month);
            Predicate yearPredicate = criteriaBuilder.equal(yearExpression, year);
            return criteriaBuilder.and(monthPredicate, yearPredicate);
        };
    }

    public static Specification<Transaction> byOrganization(Long organizationId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.equal(root.get("organization").get("id"), organizationId),
                criteriaBuilder.isNull(root.get("organization"))
        );
    }
}
