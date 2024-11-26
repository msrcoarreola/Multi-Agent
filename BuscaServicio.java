package examples.yellowPages;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.OneShotBehaviour;
import org.json.JSONObject;
import org.json.JSONArray;

public class BuscaServicio extends Agent {

    public class DataSet {
        protected double[] x;
        protected double[] exo1;
        protected double[] exo2;
        protected double[] endo;

        // Constructor para regresión simple/polinomial
        public DataSet(double[] x, double[] y) {
            this.x = x;
            this.endo = y;
        }

        // Constructor para regresión múltiple
        public DataSet(double[] x1, double[] x2, double[] y) {
            this.exo1 = x1;
            this.exo2 = x2;
            this.endo = y;
        }
    }

    public class SimpleLinearRegression {

        public void predicciones(double[] coef, double[] nuevosx) {
            for (double x : nuevosx) {
                double res = (coef[0] + coef[1] * x);
                System.out.println("y = " + coef[0] + " + " + coef[1] + " * (" + x + ") = " + res);
            }
        }
    }

    public class MultipleLinearRegression {

        public void predicciones(double[] coef, double[] nuevosVX1, double[] nuevosVX2) {
            for (int i = 0; i < 5; i += 1) {
                double b = nuevosVX1[i];
                double c = nuevosVX2[i];
                double res = (coef[0] + coef[1] * b + coef[2] * c);
                System.out.println("y = " + coef[0] + " + " + coef[1] + " ( " + b + " ) + "
                        + coef[2] + " ( " + c + " ) = " + res);

            }
        }
    }

    public class polynomialRegression {
        public void predicciones(double[] coef, double[] nuevosX) {
            for (double x : nuevosX) {
                double prediccion = coef[0] + coef[1] * x + coef[2] * Math.pow(x, 2);
                System.out.println("y = " + coef[0] + " + " + coef[1] + "* ( " + x + " ) +" +
                        coef[2] + "* ( " + x + " )^{2} = " + prediccion);
            }
        }
    }

    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado.");
        doWait(2000);
        addBehaviour(new ClasificacionYRegresionBehaviour());
    }

    private class ClasificacionYRegresionBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            // Definir el conjunto de datos
            DataSet data = new DataSet(
                    new double[] { 41.9, 43.4, 43.9, 44.5, 47.3, 47.5, 47.9, 50.2, 52.8, 53.2, 56.7, 57.0, 63.5, 65.3,
                            71.1, 77.0, 77.8 }, // x o x1 Para regresiones simples y polinomiales SALES


                    new double[] { 29.1, 29.3, 29.5, 29.7, 29.9, 30.3, 30.5, 30.7, 30.8, 30.9, 31.5, 31.7, 31.9, 32.0,
                            32.1, 32.5, 32.9 }, // x2 (solo para regresión múltiple)


                    new double[] { 251.3, 251.3, 248.3, 267.5, 273.0, 276.5, 270.3, 274.9, 285.0, 290.0, 297.0, 302.5,
                            304.5, 309.3, 321.7, 330.7, 349.0 } // y ADVERTISING
            );

            // Buscar agentes de clasificación
            AID classificationAgent = searchAgent("classification-service");
            if (classificationAgent == null) {
                System.out.println("No se encontró un agente de clasificación.");
                return;
            }

            // Enviar solicitud de clasificación
            // Crea un mensaje de solicitud (REQUEST)
            ACLMessage classMsg = new ACLMessage(ACLMessage.REQUEST);
            classMsg.addReceiver(classificationAgent);
            classMsg.setContent(serializeVariablesToJson(data));
            classMsg.setConversationId("classification-analysis");
            send(classMsg);

            // Recibir recomendación de análisis
            // espera la llegada de un mensaje dirigido
            ACLMessage classReply = blockingReceive();
            if (classReply != null && classReply.getPerformative() == ACLMessage.INFORM) {
                String analysisType = classReply.getContent();
                System.out.println("Tipo de análisis recomendado: " + analysisType);

                // Determinar el tipo de servicio de regresión
                String regressionServiceType = "";
                switch (analysisType) {
                    case "Simple Linear Regression":
                        regressionServiceType = "simple-regression-service";
                        break;
                    case "Multiple Linear Regression":
                        regressionServiceType = "multiple-regression-service";
                        break;
                    case "Polynomial Regression":
                        regressionServiceType = "polynomial-regression-service";
                        break;
                    default:
                        System.out.println("Tipo de análisis desconocido.");
                        return;
                }

                // Buscar el agente de regresión adecuado y enviar los datos
                AID regressionAgent = searchAgent(regressionServiceType);
                if (regressionAgent != null) {
                    ACLMessage regressionRequest = new ACLMessage(ACLMessage.REQUEST);
                    regressionRequest.addReceiver(regressionAgent);
                    regressionRequest.setContent(serializeVariablesToJson(data));
                    regressionRequest.setConversationId("regression-analysis");
                    send(regressionRequest);

                    // Recibir parámetros de regresión
                    ACLMessage regressionReply = blockingReceive();
                    if (regressionReply != null && regressionReply.getPerformative() == ACLMessage.INFORM) {
                        System.out.println("Parámetros de regresión recibidos: " + regressionReply.getContent());

                        String jsonContent = regressionReply.getContent();
                        double[] coeficientes = extractCoeficientesFromJson(jsonContent);

                        // Ejemplo de datos nuevos para realizar predicciones
                        double[] nuevosx1 = { 77.8, 80, 47.3, 92, 52.8 };
                        double[] nuevosx2 = { 32.9, 33.3, 29.9, 34.5, 30.8 };

                        switch (analysisType) {
                            case "Simple Linear Regression":
                                SimpleLinearRegression regression = new SimpleLinearRegression();
                                regression.predicciones(coeficientes, nuevosx1);
                                break;
                            case "Multiple Linear Regression":
                                MultipleLinearRegression regressionM = new MultipleLinearRegression();
                                regressionM.predicciones(coeficientes, nuevosx1, nuevosx2);
                                break;
                            case "Polynomial Regression":
                                polynomialRegression regressionP = new polynomialRegression();
                                regressionP.predicciones(coeficientes, nuevosx1);
                                break;

                            default:
                                break;
                        }

                    }
                } else {
                    System.out.println("No se encontró el agente de regresión adecuado.");
                }
            }
        }

        // Busca agentes y devuelve AID del primer agente encontrado
        private AID searchAgent(String serviceType) {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(serviceType);
            template.addServices(sd);
            try {
                DFAgentDescription[] results = DFService.search(myAgent, template);
                if (results.length > 0)
                    return results[0].getName();
            } catch (FIPAException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    // Transformar el arreglo obtenido del agente de regresion a un arreglo
    private double[] extractCoeficientesFromJson(String jsonContent) {
        JSONObject jsonObject = new JSONObject(jsonContent);
        JSONArray coeficientesArray = jsonObject.getJSONArray("Coeficientes");
        double[] coeficientes = new double[coeficientesArray.length()];

        for (int i = 0; i < coeficientesArray.length(); i++) {
            coeficientes[i] = coeficientesArray.getDouble(i);
        }
        return coeficientes;
    }

    // Convertir la data a formato json
    private String serializeVariablesToJson(DataSet data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        if (data.x != null) {
            sb.append("\"x\": [");
            for (int i = 0; i < data.x.length; i++) {
                sb.append(data.x[i]);
                if (i < data.x.length - 1)
                    sb.append(", ");
            }
            sb.append("], ");
        }

        if (data.exo1 != null) {
            sb.append("\"x1\": [");
            for (int i = 0; i < data.exo1.length; i++) {
                sb.append(data.exo1[i]);
                if (i < data.exo1.length - 1)
                    sb.append(", ");
            }
            sb.append("], ");
        }

        if (data.exo2 != null) {
            sb.append("\"x2\": [");
            for (int i = 0; i < data.exo2.length; i++) {
                sb.append(data.exo2[i]);
                if (i < data.exo2.length - 1)
                    sb.append(", ");
            }
            sb.append("], ");
        }

        sb.append("\"y\": [");
        for (int i = 0; i < data.endo.length; i++) {
            sb.append(data.endo[i]);
            if (i < data.endo.length - 1)
                sb.append(", ");
        }
        sb.append("] }"); // Asegura que el JSON termine con "}"

        return sb.toString();
    }

}
