package edu.kit.kastel.sdq.lissa.ratlr.classifier.services;

public enum Service {
    ;

    private final Class<?> serviceClass;

    Service(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }
}
