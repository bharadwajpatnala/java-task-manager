package app.util;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
public final class DateUtil {
    private DateUtil(){}
    public static boolean isValidDate(String date) {
        if(date==null || date.isBlank())
            return true;
        try {
            return LocalDate.parse(date).toString().equals(date);
        }
        catch(DateTimeParseException ex)
        {
            return false;
        }
    }
}
