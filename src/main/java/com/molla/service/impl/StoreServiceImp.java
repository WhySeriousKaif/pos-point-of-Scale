package com.molla.service.impl;

import com.molla.domain.StoreStatus;
import com.molla.exceptions.UserException;
import com.molla.mapper.StoreMapper;
import com.molla.model.Store;
import com.molla.model.StoreContact;
import com.molla.model.User;
import com.molla.payload.dto.StoreDto;
import com.molla.repository.StoreRepository;
import com.molla.service.StoreService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StoreServiceImp implements StoreService {

    private  final StoreRepository storeRepository;
    private final UserService userService;
    @Override
    public StoreDto createStore(StoreDto storeDto, User user) throws UserException {
        // Check if user already has a store
        Store existingStore = storeRepository.findByStoreAdminId(user.getId());
        if(existingStore != null) {
            throw new UserException("User already has a store. One user can only have one store.");
        }
        
        Store store = StoreMapper.toEntity(storeDto, user);
        return StoreMapper.toDTO(storeRepository.save(store));
    }

    @Override
    public StoreDto getStoreById(Long id) {
        Store store=storeRepository.findById(id).orElseThrow(() -> new RuntimeException("Store not found"));
        return StoreMapper.toDTO(store);
    }

    @Override
    public List<StoreDto> getAllStores() {
        List<Store> dtos=storeRepository.findAll();
        return dtos.stream().map(StoreMapper::toDTO).collect(Collectors.toList());

    }

    @Override
    public StoreDto getStoreByAdmin(User user) throws UserException {
        User admin = userService.getCurrentUser();
        Store store = storeRepository.findByStoreAdminId(admin.getId());
        if(store == null) {
            throw new UserException("Store not found");
        }
        return StoreMapper.toDTO(store);
    }

    @Override
    public StoreDto updateStore(Long id, StoreDto storeDto) throws UserException {
        User currentUser=userService.getCurrentUser();
        Store existingStore=storeRepository.findByStoreAdminId(currentUser.getId());
        if(existingStore==null){
            throw new UserException("Store not found");
        }
        existingStore.setBrand(storeDto.getBrand());
        existingStore.setDescription(storeDto.getDescription());
        if(storeDto.getStoreType()!=null){
            existingStore.setStoreType(storeDto.getStoreType());
        }
        
        if(storeDto.getContact()!=null){
           StoreContact contact=StoreContact.builder().address(storeDto.getContact().getAddress()).phone(storeDto.getContact().getPhone()).email(storeDto.getContact().getEmail()).build();
           existingStore.setContact(contact);
        }
      Store updatedStore=storeRepository.save(existingStore);
      return StoreMapper.toDTO(updatedStore);
    }

    @Override
    public void deleteStore(Long id) throws UserException {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new UserException("Store not found"));
        storeRepository.delete(store);
    }

    @Override
    public StoreDto getStoreByEmployee(String brand) throws UserException {
        User currentUser = userService.getCurrentUser();
        if(currentUser == null){
            throw new UserException("You don't have permission to access this store"); 
        }
        Store store = currentUser.getStore();
        if(store == null) {
            throw new UserException("Store not found for this employee");
        }
        return StoreMapper.toDTO(store);
    }
    @Override
    public StoreDto moderateStore(Long id, StoreStatus storeStatus) {
        Store store=storeRepository.findById(id).orElseThrow(() -> new RuntimeException("Store not found"));
        store.setStoreStatus(storeStatus);
        return StoreMapper.toDTO(storeRepository.save(store));
    } 
}
