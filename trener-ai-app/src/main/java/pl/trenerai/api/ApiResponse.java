package pl.trenerai.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standardowa odpowiedź API")
public record ApiResponse(
        @Schema(description = "Komunikat wyniku operacji", example = "Operacja zakończona pomyślnie")
        String message) {
}
