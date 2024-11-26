package examples.yellowPages;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Arrays;

public class OfreceServicio extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado.");

        // Registrar el servicio de clasificación una sola vez
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("classification-service");
        sd.setName("classification-service");
        dfd.addServices(sd);
        try {
            // registra el agente
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Añadir comportamiento para recibir mensajes de otros agentes
        addBehaviour(new OfreceClasificacionBehaviour());
    }

    @Override
    // Desregistra al agente del DF y notifica que ya no ofrece sus servicios.
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("El agente " + getAID().getName() + " ya no ofrece sus servicios.");
    }

    // El comportamiento cíclico permite al agente recibir mensajes continuamente y
    // determinar el tipo de regresión adecuado.
    private class OfreceClasificacionBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            // recibe el mensaje y determina si no es nulo
            if (msg != null && msg.getConversationId().equals("classification-analysis")) {

                // Extrae el arreglo en formato JSON
                JSONObject json = new JSONObject(msg.getContent());
                JSONArray yArray = json.getJSONArray("y");
                double[] yValues = parseJsonArray(yArray);

                double[] x1Values = null;
                double[] x2Values = null;

                // Verificación de regresión múltiple o simple/polinómica
                // Y extrae los datos de los arreglos de x
                if (json.has("x1") && json.has("x2") && json.getJSONArray("x2").length() > 0) {
                    x1Values = parseJsonArray(json.getJSONArray("x1"));
                    x2Values = parseJsonArray(json.getJSONArray("x2"));
                } else if (json.has("x1")) {
                    x1Values = parseJsonArray(json.getJSONArray("x1"));
                }

                // Determinar tipo de regresión
                String regressionType = classifyRegression(x1Values, x2Values, yValues);

                // Enviar tipo de análisis recomendado
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(regressionType);
                send(reply);
                System.out.println("Análisis recomendado enviado: " + regressionType);

            } else {
                block(); // Block until new messages arrive
            }
        }

        // convierte un JSONArray (arreglo en formato JSON) a un arreglo de double en
        // Java
        private double[] parseJsonArray(JSONArray jsonArray) {
            double[] array = new double[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                array[i] = jsonArray.getDouble(i);
            }
            return array;
        }
    }

    private String classifyRegression(double[] x1Values, double[] x2Values, double[] yValues) {
        if (x2Values != null && x2Values.length > 0) {
            // Regresión múltiple
            double correlationX1 = calculatePearsonCorrelation(x1Values, yValues);
            double correlationX2 = calculatePearsonCorrelation(x2Values, yValues);
            return "Multiple Linear Regression";
        } else if (x1Values != null) {
            // Calcular coorrelacuion simple o polinómica
            double correlationLinear = calculatePearsonCorrelation(x1Values, yValues);

            // Calcular la correlación cuadrática
            double[] x1Squared = Arrays.stream(x1Values).map(x -> x * x).toArray();
            double correlationQuadratic = calculatePearsonCorrelation(x1Squared, yValues);

            // Imprimir correlaciones para depuración
            System.out.println("Correlación lineal: " + correlationLinear);
            System.out.println("Correlación cuadrática (polinomial): " + correlationQuadratic);

            // Priorizar la regresión polinomial si la correlación cuadrática es
            // significativamente mayor
            if (Math.abs(correlationQuadratic) > Math.abs(correlationLinear) + 0.01) {
                return "Polynomial Regression";
            } else if (Math.abs(correlationLinear) > 0.85) {
                return "Simple Linear Regression";
            }
        }
        return "Unknown Regression Type";
    }

    private double calculatePearsonCorrelation(double[] x, double[] y) {
        // calcula el promedio de los elementos x y y
        double meanX = Arrays.stream(x).average().orElse(0);
        double meanY = Arrays.stream(y).average().orElse(0);

        double numerator = 0.0;
        double denominatorX = 0.0;
        double denominatorY = 0.0;

        // Realiza las sumatorias respectivas
        for (int i = 0; i < x.length; i++) {
            numerator += (x[i] - meanX) * (y[i] - meanY);
            denominatorX += Math.pow(x[i] - meanX, 2);
            denominatorY += Math.pow(y[i] - meanY, 2);
        }

        // Evitar la división por cero
        if (denominatorX == 0 || denominatorY == 0) {
            return 0;
        }

        return numerator / Math.sqrt(denominatorX * denominatorY);
    }
}
