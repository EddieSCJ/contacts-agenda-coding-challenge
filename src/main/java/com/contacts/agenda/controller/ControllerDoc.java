package com.contacts.agenda.controller;

public final class ControllerDoc {

    private ControllerDoc() {}

    public static final class Contacts {
        public static final String TAG_NAME = "Contacts";
        public static final String TAG_DESCRIPTION = "Contact management API with resilient external service integration";

        public static final String GET_ALL_SUMMARY = "Get all contacts";
        public static final String GET_ALL_DESCRIPTION = """
                Retrieves all contacts with automatic high availability and performance optimization.
                
                The API intelligently manages data sources to ensure fast response times and reliability,
                even when external dependencies are temporarily unavailable.
                
                If you are able to break it or find failing scenarios, please let me know. I would love to hear about them.
                """;

        public static final String RESPONSE_200_DESCRIPTION = "Successfully retrieved contacts from API or fallback storage";
    }

    public static final class ErrorResponses {
        public static final String RESPONSE_503_DESCRIPTION = "Service unavailable - all sources of data are unavailable";
        public static final String RESPONSE_500_DESCRIPTION = "Internal server error - unexpected error occurred";
    }
}
