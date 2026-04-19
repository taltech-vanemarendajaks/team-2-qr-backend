package ee.valiit.mystuffback.controller.item;


import ee.valiit.mystuffback.controller.item.dto.ItemBasicInfo;
import ee.valiit.mystuffback.controller.item.dto.ItemDto;
import ee.valiit.mystuffback.infrastructure.error.ApiError;
import ee.valiit.mystuffback.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/item")
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    @Operation(summary = "Addition of a new item", description = "itemName and Date are mandatory fields")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created"),
            })
    public ResponseEntity<Void> addItem(@RequestBody @Valid ItemDto itemDto) {
        itemService.addItem(itemDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/all")
    @Operation(summary = "Returns a list of user's items by date and name.")
    public List<ItemBasicInfo> findItems() {
        return itemService.findItems();
    }

    @GetMapping
    @Operation(summary = "Returns all details of a chosen item")
    public ItemDto findItem(@RequestParam Integer itemId) {
        return itemService.findItem(itemId);
    }

    @PutMapping
    @Operation(summary = "Changes the details of an existing item")
    public void updateItemInfo(@RequestParam Integer itemId, @RequestBody ItemDto itemDto) {
        itemService.updateItemInfo(itemId, itemDto);
    }

    @DeleteMapping
    @Operation(summary = "Removes item from system")
    public void removeItem(@RequestParam Integer itemId) {
        itemService.removeItem(itemId);
    }

    @DeleteMapping("/{itemId}/images/{imageId}")
    public ResponseEntity<Void> removeItemImage(
            @PathVariable Integer itemId,
            @PathVariable Integer imageId) {
        itemService.removeItemImage(itemId, imageId);
        return ResponseEntity.noContent().build();
    }
}

