package api.ailawyer.uz.util;

/**
 * float[] vektorni PostgreSQL pgvector literal formatiga aylantiradi: [0.1,0.2,...]
 */
public final class PgVectorUtils {

    private PgVectorUtils() {}

    public static String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
