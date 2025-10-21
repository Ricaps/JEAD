package cz.muni.jena.issue;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum IssueType
{
    UNUSED_INJECTION(IssueCategory.DI, 1),
    DIRECT_CONTAINER_CALL(IssueCategory.DI, 2),
    CONCRETE_CLASS_INJECTION(IssueCategory.DI, 3),
    OPEN_WINDOW_INJECTION(IssueCategory.DI, 4),
    FRAMEWORK_COUPLING(IssueCategory.DI, 5),
    MULTIPLE_FORMS_OF_INJECTION(IssueCategory.DI, 6),
    OPEN_DOOR_INJECTION(IssueCategory.DI, 7),
    GOD_DI_CLASS(IssueCategory.DI, 8),
    MULTIPLE_ASSIGNED_INJECTION(IssueCategory.DI, 9),
    LONG_PRODUCER_METHOD(IssueCategory.DI, 10),
    FINAL_METHOD_CALL_WITH_EXCEPTION(IssueCategory.MOCKING, 1),
    CONSTRUCTOR_CALL_WITH_EXCEPTION(IssueCategory.MOCKING, 2),
    STATIC_METHOD_CALL_WITH_EXCEPTION(IssueCategory.MOCKING, 3),
    INAPPROPRIATE_METHOD_MOCKING(IssueCategory.MOCKING, 4),
    INAPPROPRIATE_METHOD_CALL_IN_STATIC_BLOCK(IssueCategory.MOCKING, 5),
    STORING_SECRETS_IN_INSECURE_PLACES(IssueCategory.SECURITY, 1),
    DISABLING_CSRF_PROTECTION(IssueCategory.SECURITY, 2),
    LIFELONG_ACCESS_TOKENS(IssueCategory.SECURITY, 3),
    INSECURE_DEFAULT_CONFIGURATION(IssueCategory.SECURITY, 4),
    SIGNING_JWT_WITH_FIXED_SECRET(IssueCategory.SECURITY, 5),
    INSECURE_COMMUNICATION(IssueCategory.SECURITY, 6),
    TINY_SERVICE(IssueCategory.SERVICE_LAYER, 1),
    MULTI_SERVICE(IssueCategory.SERVICE_LAYER, 2),
    N_PLUS1_QUERY_PROBLEM(IssueCategory.PERSISTENCE, 1),
    SELF_ADMITTED_TECHNICAL_DEBT(IssueCategory.TECHNICAL_DEBT, 1),
    COMMENTED_OUT_CODE(IssueCategory.TECHNICAL_DEBT, 2);

    private final IssueCategory category;
    private final Integer order;

    @Override
    public String toString()
    {
        return category.toString() + order + " " +
                Arrays.stream(name().toLowerCase(Locale.ROOT).split("_"))
                .map(string -> string.substring(0, 1).toUpperCase(Locale.ROOT) + string.substring(1))
                .collect(Collectors.joining(" "));
    }

    IssueType(IssueCategory category, Integer order)
    {
        this.category = category;
        this.order = order;
    }

    public IssueCategory getCategory()
    {
        return category;
    }

    public Integer getOrder()
    {
        return order;
    }
}
