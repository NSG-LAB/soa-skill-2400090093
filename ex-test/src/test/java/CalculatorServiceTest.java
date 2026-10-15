import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculatorServiceTest {

    CalculatorService service = new CalculatorService();

    @Test
    void testAdd() {
        int result = service.add(10, 20);
        assertEquals(30, result);
    }

    @Test
    void testMul() {
        int result = service.multiply(5, 4);
        assertEquals(20, result);
    }
}
