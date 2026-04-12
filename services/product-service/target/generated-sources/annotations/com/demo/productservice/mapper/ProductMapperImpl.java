package com.demo.productservice.mapper;

import com.demo.productservice.dto.ProductRequest;
import com.demo.productservice.dto.ProductResponse;
import com.demo.productservice.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-11T19:40:32+0200",
    comments = "version: 1.6.0, compiler: javac, environment: Java 21.0.10 (Ubuntu)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductResponse toResponse(Product product) {
        if ( product == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String description = null;
        BigDecimal price = null;
        Integer stock = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        id = product.getId();
        name = product.getName();
        description = product.getDescription();
        price = product.getPrice();
        stock = product.getStock();
        createdAt = product.getCreatedAt();
        updatedAt = product.getUpdatedAt();

        ProductResponse productResponse = new ProductResponse( id, name, description, price, stock, createdAt, updatedAt );

        return productResponse;
    }

    @Override
    public Product toProduct(ProductRequest request) {
        if ( request == null ) {
            return null;
        }

        Product.ProductBuilder product = Product.builder();

        product.name( request.name() );
        product.description( request.description() );
        product.price( request.price() );
        product.stock( request.stock() );

        return product.build();
    }

    @Override
    public void updateEntity(ProductRequest request, Product entity) {
        if ( request == null ) {
            return;
        }

        entity.setName( request.name() );
        entity.setDescription( request.description() );
        entity.setPrice( request.price() );
        entity.setStock( request.stock() );
    }
}
