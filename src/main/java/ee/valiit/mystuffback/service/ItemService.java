package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.controller.item.dto.ItemBasicInfo;
import ee.valiit.mystuffback.controller.item.dto.ItemDto;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.infrastructure.exception.PrimaryKeyNotFoundException;
import ee.valiit.mystuffback.infrastructure.status.Status;
import ee.valiit.mystuffback.infrastructure.util.BytesConverter;
import ee.valiit.mystuffback.persistence.item.Item;
import ee.valiit.mystuffback.persistence.item.ItemMapper;
import ee.valiit.mystuffback.persistence.item.ItemRepository;
import ee.valiit.mystuffback.persistence.itemimage.ItemImage;
import ee.valiit.mystuffback.persistence.itemimage.ItemImageRepository;
import ee.valiit.mystuffback.persistence.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ee.valiit.mystuffback.infrastructure.error.Error.ITEM_NAME_UNAVAILABLE;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final ItemImageRepository itemImageRepository;
    private final UserService userService;


    @Transactional
    public void addItem(Integer userId, ItemDto itemDto) {
        validateItemNameIsAvailable(itemDto.getItemName());
        User user = userService.getValidUser(userId);
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

    private static ItemImage createItemImage(Item item, String imageData) {
        ItemImage itemImage = new ItemImage();
        itemImage.setItem(item);
        itemImage.setImageData(BytesConverter.stringToBytes(imageData));
        return itemImage;
    }

    private void validateItemNameIsAvailable(String itemName) {
        boolean itemExists = itemRepository.itemExistsBy(itemName);
        if (itemExists) {
            throw new ForbiddenException(ITEM_NAME_UNAVAILABLE.getMessage(), ITEM_NAME_UNAVAILABLE.getErrorCode());
        }
    }

    public List<ItemBasicInfo> findItems(Integer userId) {
        List<Item> items = itemRepository.findActiveItemsBy(userId);
        return itemMapper.toItemBasicInfos(items);
    }

    public ItemDto findItem(Integer itemId) {
        Item item = getValidItem(itemId);
        ItemDto itemDto = itemMapper.toItemDto(item);
        handleAddImageDataToItemDto(itemId, itemDto);
        return itemDto;
    }

    private void handleAddImageDataToItemDto(Integer itemId, ItemDto itemDto) {
        Optional<ItemImage> optionalItemImage = itemImageRepository.findItemImageBy(itemId);
        if (optionalItemImage.isPresent()) {
            ItemImage itemImage = optionalItemImage.get();
            addImageDataToItemDto(itemImage, itemDto);
        }
    }

    private void addImageDataToItemDto(ItemImage itemImage, ItemDto itemDto) {
        byte[] itemImageData = itemImage.getImageData();
        itemDto.setImageData(BytesConverter.bytesToString(itemImageData));
        itemDto.setImageId(itemImage.getId());
    }


    public Item getValidItem(Integer itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new PrimaryKeyNotFoundException("itemId", itemId));
    }

    public Item updateItemInfo(Integer itemId, ItemDto itemDto) {
        Item item = getValidItem(itemId);
        updateItemImage(itemDto, item);
        itemMapper.updateItem(item, itemDto);
        itemRepository.save(item);
        return item;
    }

    private void updateItemImage(ItemDto itemDto, Item item) {
        itemImageRepository.deleteItemImagesBy(item);
        handleAddItemImage(item, itemDto.getImageData());
    }

    public void removeItem(Integer itemId) {
        Item item = getValidItem(itemId);
        itemImageRepository.deleteItemImagesBy(item);
        item.setStatus(Status.SOFT_DELETED.getCode());
        itemRepository.save(item);
    }

    public void removeItemImage(Integer itemId, Integer imageId) {
        Item item = getValidItem(itemId);
        ItemImage image =itemImageRepository.findById(imageId)
                .orElseThrow(() -> new PrimaryKeyNotFoundException("imageId", imageId));
        if (!image.getItem().getId().equals(itemId)) {
            throw new IllegalArgumentException("Image does not belong to this item");
        }
        itemImageRepository.deleteById(imageId);
    }
}

