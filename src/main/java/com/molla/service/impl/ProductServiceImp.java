package com.molla.service.impl;

import com.molla.model.Category;
import com.molla.model.Product;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.mapper.ProductMapper;
import com.molla.payload.dto.ProductDto;
import com.molla.repository.CategoryRepository;
import com.molla.repository.ProductRepository;
import com.molla.repository.StoreRepository;
import com.molla.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImp implements ProductService {
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    @Override
    public ProductDto createProduct(ProductDto productDto, User user) {
       Store store=storeRepository.findById(
        productDto.getStoreId()
       ).orElseThrow(() -> new RuntimeException("Store not found"));
       Category category=categoryRepository.findById(productDto.getCategoryId()).orElseThrow(() -> new RuntimeException("Category not found"));
       Product product=ProductMapper.toEntity(productDto,store,category);
       Product savedProduct=productRepository.save(product);



       return ProductMapper.toDto(savedProduct);
    }

    @Override
    public ProductDto updateProduct(Long id, ProductDto productDto, User user) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setSku(productDto.getSku());
        product.setMrp(productDto.getMrp());
        product.setSellingPrice(productDto.getSellingPrice());
        // Update price to match sellingPrice if price column exists in database
        if(productDto.getSellingPrice() != null) {
            product.setPrice(productDto.getSellingPrice());
        }
        // Update quantity if provided
        if(productDto.getQuantity() != null) {
            product.setQuantity(productDto.getQuantity());
        }
        product.setBrand(productDto.getBrand());
        product.setImage(productDto.getImage());
        if(productDto.getStoreId() != null) {
            Store store = storeRepository.findById(productDto.getStoreId())
                    .orElseThrow(() -> new RuntimeException("Store not found"));
            product.setStore(store);
        }
        Product savedProduct = productRepository.save(product);
        return ProductMapper.toDto(savedProduct);
    }

    @Override
    public void deleteProduct(Long id, User user) {
        Product product=productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        productRepository.delete(product);

    }

    @Override
    public List<ProductDto> getProductsById(Long storeId) {
    List<Product> products=productRepository.findByStoreId(storeId);
    return products.stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ProductDto> searchByKeyword(Long storeId, String keyword) {
    List<Product> products=productRepository.searchByKeyword(storeId,keyword);
    return products.stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ProductDto> getAllProducts(Long storeId) {
    List<Product> products=productRepository.findByStoreId(storeId);
    return products.stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }
}
