package com.ssafy.backend.service;

import com.ssafy.backend.db.entity.Nft;
import com.ssafy.backend.db.entity.Salelog;
import com.ssafy.backend.db.entity.User;
import com.ssafy.backend.db.repository.NFTRepository;
import com.ssafy.backend.db.repository.SalelogRepository;
import com.ssafy.backend.db.repository.UserLikeRepository;
import com.ssafy.backend.db.repository.UserRepository;
import com.ssafy.backend.dto.NFTCreateDto;
import com.ssafy.backend.dto.NFTDto;
import com.ssafy.backend.dto.SaleResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class NFTServiceImpl implements NFTService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    NFTRepository nftRepository;
    @Autowired
    SalelogRepository salelogRepository;
    @Autowired
    UserLikeRepository userLikeRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private AwsS3Service awsS3Service;

    private final String gatewayURL = "https://gateway.pinata.cloud/ipfs/";

    @Override
    public Nft findById(int id) {
        return nftRepository.findById(id);
    }

    @Override
    public Nft findBycontractAddress(String contract_address) {
        return nftRepository.findByContractAddress(contract_address);
    }

    @Override
    public void modifyNftOwner(SaleResultDto saleResultDto) {
        int nftId = saleResultDto.getNftId();
        String buyerWallerAddress = saleResultDto.getBuyerWalletAddress();
        Nft nft = nftRepository.findById(nftId);
        nft.setOwner(userService.findByUserWalletAddress(buyerWallerAddress));
        nftRepository.save(nft);
    }

    @Override
    public List<NFTDto> getCollectedNft(String wallet_address) {
        User user = userRepository.findByWalletAddress(wallet_address).orElse(null);
        if(user == null) {
            throw new IllegalArgumentException("No such User");
        }

        List<Nft> nftList = nftRepository.findOwnedNfts(user);
        return makeNFTDtoList(nftList);
    }

    @Override
    public List<NFTDto> getCreatedNft(String wallet_address) {
        User user = userRepository.findByWalletAddress(wallet_address).orElse(null);
        if(user == null) {
            throw new IllegalArgumentException("No such User");
        }

        List<Nft> nftList = nftRepository.findCreatedNfts(user);
        return makeNFTDtoList(nftList);
    }

    @Override
    public List<NFTDto> getOnSaleNft(String wallet_address) {
        User user = userRepository.findByWalletAddress(wallet_address).orElse(null);
        if(user == null) {
            throw new IllegalArgumentException("No such User");
        }

        List<Nft> nftList = nftRepository.findOnSaleNfts(user);
        return makeNFTDtoList(nftList);
    }

    @Override
    public List<NFTDto> getLikedNft(String wallet_address) {
        User user = userRepository.findByWalletAddress(wallet_address).orElse(null);
        if(user == null) {
            throw new IllegalArgumentException("No such User");
        }

        List<Nft> nftList = nftRepository.findLikedNfts(user);
        return makeNFTDtoList(nftList);
    }

    @Override
    @Transactional
    public void postNFT(NFTCreateDto dto) throws IllegalArgumentException{
        System.out.println("postNFT");
        User user = userRepository.findByWalletAddress(dto.getCreatorWalletAddress()).orElse(null);
        if(user == null){
            throw new IllegalArgumentException("No such user");
        }

        LocalDateTime now = LocalDateTime.now();
        Instant instant = now.atZone(ZoneId.systemDefault()).toInstant();
        Nft nft = Nft.builder()
                .creator(user)
                .owner(user)
                .contractAddress(dto.getContractAddress())
                .nftHash(dto.getTxHash())
                .nftTitle(dto.getNftTitle())
                .nftDescription(dto.getNftDescription())
                .nftTag(dto.getNftTag())
                .isSale(false)
                .tokenCreatedate(Date.from(instant))
                .ipfsUrl(gatewayURL+dto.getCid())
                .build();

        awsS3Service.uploadNFTImage(nft, dto.getImgFile());
        nftRepository.save(nft);
    }

    @Override
    @Transactional
    public void updatePossessed(int nftId) {
        Nft nft = nftRepository.findById(nftId);
        if(nft == null){
            throw new IllegalArgumentException("No such NFT");
        }

        nft.setSale(false);
        nft.setPrice(null);
    }

    @Override
    @Transactional
    public void updateOnSale(int nftId, String price) {
        Nft nft = nftRepository.findById(nftId);
        if(nft == null){
            throw new IllegalArgumentException("No such NFT");
        }

        nft.setSale(true);
        nft.setPrice(price);
    }

    @Override
    public List<NFTDto> getAllNFT() {
        List<Nft> nftList = nftRepository.findAll();
        return makeNFTDtoList(nftList);
    }

    private List<NFTDto> makeNFTDtoList(List<Nft> nftList){
        List<NFTDto> dtoList = new ArrayList<>();
        for(Nft nft : nftList){
            List<String> tagList = new ArrayList<>();
            String[] tags = nft.getNftTag().split(" ");
            for(int i=0; i<tags.length; i++){
                tagList.add(tags[i]);
            }

            String lastPrice = null;
            Salelog log = salelogRepository.findFirstByNftOrderByDateDesc(nft);
            if(log != null){
                lastPrice = salelogRepository.findFirstByNftOrderByDateDesc(nft).getPrice();
            }
            int likes = userLikeRepository.getLikeCountOfNft(nft);
            dtoList.add(buildNFTDto(nft, tagList, lastPrice, likes));
        }

        return dtoList;
    }
    private NFTDto buildNFTDto(Nft nft, List<String> tagList, String lastPrice, int likes){
        return NFTDto.builder()
                .imgUrl(nft.getImageUrl())
                .nftTitle(nft.getNftTitle())
                .nftPrice(nft.getPrice())
                .nftCreatorNickname(nft.getCreator().getUserNickname())
                .lastPrice(lastPrice)
                .nftOwnerNickname(nft.getOwner().getUserNickname())
                .nftTags(tagList)
                .nftLike(likes)
                .build();
    }
}
