package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.controller.item.dto.ItemBasicInfo;
import ee.valiit.mystuffback.controller.item.dto.ItemDto;
import ee.valiit.mystuffback.infrastructure.exception.PrimaryKeyNotFoundException;
import ee.valiit.mystuffback.infrastructure.status.Status;
import ee.valiit.mystuffback.persistence.item.Item;
import ee.valiit.mystuffback.persistence.item.ItemMapper;
import ee.valiit.mystuffback.persistence.item.ItemRepository;
import ee.valiit.mystuffback.persistence.itemimage.ItemImage;
import ee.valiit.mystuffback.persistence.itemimage.ItemImageRepository;
import ee.valiit.mystuffback.persistence.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final ItemImageRepository itemImageRepository;
    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final ImageProcessingService imageProcessingService;


    @Transactional
    public void addItem(ItemDto itemDto) {
        User user = userService.getAuthenticatedUser();
        rateLimitService.checkRateLimitOrThrow(
                "create-item",
                String.valueOf(user.getId()),
                10,
                60
        );
        Item item = itemMapper.toItem(itemDto);
        item.setUser(user);
        item.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        itemRepository.save(item);
        handleAddItemImage(item, itemDto.getImageData());
    }

    private void handleAddItemImage(Item item, String imageData) {
        if (hasImage(imageData)) {
            createAndSaveItemImage(item, imageData);
        }
    }

    private static boolean hasImage(String imageData) {
        return imageData != null && !imageData.isBlank();
    }

    private void createAndSaveItemImage(Item item, String imageData) {
        ItemImage itemImage = createItemImage(item, imageData);
        itemImageRepository.save(itemImage);
    }

    private ItemImage createItemImage(Item item, String imageData) {
        ItemImage itemImage = new ItemImage();
        itemImage.setItem(item);
        byte[] processedImageBytes = imageProcessingService.processBase64Image(imageData);
        itemImage.setImageData(processedImageBytes);
        return itemImage;
    }

    public List<ItemBasicInfo> findItems() {
        User user = userService.getAuthenticatedUser();
        List<Item> items = itemRepository.findActiveItemsByUserId(user.getId());
        return itemMapper.toItemBasicInfos(items);
    }

    public ItemDto findItem(Integer itemId) {
        Item item = getValidUserItem(itemId);
        ItemDto itemDto = itemMapper.toItemDto(item);
        handleAddImageDataToItemDto(itemId, itemDto);
        return itemDto;
    }
    private Item getValidUserItem(Integer itemId) {
        User user = userService.getAuthenticatedUser();

        return itemRepository.findActiveItemByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new PrimaryKeyNotFoundException("itemId", itemId));
    }

    private void handleAddImageDataToItemDto(Integer itemId, ItemDto itemDto) {
        Optional<ItemImage> optionalItemImage = itemImageRepository.findItemImageBy(itemId);
        optionalItemImage.ifPresent(itemImage -> addImageDataToItemDto(itemImage, itemDto));
    }

    private void addImageDataToItemDto(ItemImage itemImage, ItemDto itemDto) {
        byte[] itemImageData = itemImage.getImageData();
        String base64Image = Base64.getEncoder().encodeToString(itemImageData);
        itemDto.setImageData("data:image/jpeg;base64," + base64Image);
        itemDto.setImageId(itemImage.getId());
    }

    public void updateItemInfo(Integer itemId, ItemDto itemDto) {
        Item item = getValidUserItem(itemId);
        updateItemImage(itemDto, item);
        itemMapper.updateItem(item, itemDto);
        itemRepository.save(item);
    }

    private void updateItemImage(ItemDto itemDto, Item item) {
        boolean removeImage = Boolean.TRUE.equals(itemDto.getRemoveImage());
        boolean hasNewImage = hasImage(itemDto.getImageData());

        if (removeImage) {
            itemImageRepository.deleteItemImagesBy(item);
            return;
        }

        if (hasNewImage) {
            itemImageRepository.deleteItemImagesBy(item);
            handleAddItemImage(item, itemDto.getImageData());
        }

        // else → do nothing (keep existing image)
    }

    public void removeItem(Integer itemId) {
        Item item = getValidUserItem(itemId);
        itemImageRepository.deleteItemImagesBy(item);
        item.setStatus(Status.SOFT_DELETED.getCode());
        itemRepository.save(item);
    }

    public void removeItemImage(Integer itemId, Integer imageId) {
        getValidUserItem(itemId);
        ItemImage image = itemImageRepository.findById(imageId)
                .orElseThrow(() -> new PrimaryKeyNotFoundException("imageId", imageId));
        if (!image.getItem().getId().equals(itemId)) {
            throw new PrimaryKeyNotFoundException("imageId", imageId);
        }
        itemImageRepository.deleteById(imageId);
    }
}

