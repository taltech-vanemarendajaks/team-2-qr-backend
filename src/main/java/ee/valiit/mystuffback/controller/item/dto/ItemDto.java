package ee.valiit.mystuffback.controller.item.dto;

import ee.valiit.mystuffback.persistence.item.Item;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO for {@link Item}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDto implements Serializable {

    @NotNull
    @Size(max = 50)
    private String itemName;

    @NotNull
    private LocalDate itemDate;

    @Size(max = 250)
    private String model;

    @Size(max = 500)
    private String comment;

    private String imageData;

    private Integer imageId;

    private String qrToken;

    private Boolean removeImage;

    private LocalDate warrantyEndDate;

    private Instant warrantyNotifyAt;
}
