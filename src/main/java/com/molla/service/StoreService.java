package com.molla.service;

import com.molla.domain.StoreStatus;
import com.molla.exceptions.UserException;
import com.molla.model.User;
import com.molla.payload.dto.StoreDto;

import java.util.List;

public interface StoreService {

   StoreDto createStore(StoreDto storeDto, User user) throws UserException;

   StoreDto getStoreById(Long id);

   List<StoreDto> getAllStores();

   StoreDto getStoreByAdmin(User user) throws UserException;

   StoreDto updateStore(Long id, StoreDto storeDto) throws UserException;

   void deleteStore(Long id) throws UserException;
   
   StoreDto getStoreByEmployee(String brand) throws UserException;

   StoreDto moderateStore(Long id, StoreStatus storeStatus);


}
