package ee.valiit.mystuffback.persistence.item;

import ee.valiit.mystuffback.controller.item.dto.ItemBasicInfo;
import ee.valiit.mystuffback.controller.item.dto.ItemDto;
import ee.valiit.mystuffback.infrastructure.status.Status;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", imports = {Status.class})
public interface ItemMapper {


    @Mapping(source = "id", target = "itemId")
    @Mapping(source = "name", target = "itemName")
    @Mapping(source = "date", target = "itemDate")
    ItemBasicInfo toItemBasicInfo(Item item);


    List<ItemBasicInfo> toItemBasicInfos(List<Item> items);


    @Mapping(source = "name", target = "itemName")
    @Mapping(source = "date", target = "itemDate")
    @Mapping(constant = "", target = "imageData")
    @Mapping(target = "imageId", ignore = true)
    ItemDto toItemDto(Item item);

    @Mapping(target = "id", ignore = true)      // new item, id generated
    @Mapping(target = "user", ignore = true)    // we set it in service
    @Mapping(source = "itemName", target = "name")
    @Mapping(source = "itemDate", target = "date")
    @Mapping(expression = "java(Status.ACTIVE.getCode())", target = "status")
    Item toItem(ItemDto itemDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @InheritConfiguration(name = "toItem")
    @Mapping(ignore = true, target = "status")
    @Mapping(ignore = true, target = "qrToken")
    Item updateItem(@MappingTarget Item item, ItemDto itemDto);
}