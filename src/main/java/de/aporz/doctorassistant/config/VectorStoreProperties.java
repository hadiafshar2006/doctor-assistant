package de.aporz.doctorassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vector-store")
public class VectorStoreProperties {

    private final Store medical = new Store();
    private final Store patient = new Store();

    public Store getMedical() {
        return medical;
    }

    public Store getPatient() {
        return patient;
    }

    public static class Store {
        private String tableName;
        private int dimension = 768; // default from FAQ

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }
    }
}

