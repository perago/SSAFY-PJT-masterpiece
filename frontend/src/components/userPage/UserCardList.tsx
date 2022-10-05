import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { ButtonGroup, Button } from '@mui/material';
import styled from 'styled-components';
import { Link } from 'react-router-dom';
import {
  fetchCollected,
  fetchCreated,
  fetchFavorite,
  fetchInventory,
  fetchOnsale,
  Hangul,
} from '../../_slice/UserSlice';
import { NFT } from '../../_slice/NFTSlice';
import NFTCard from '../../commons/NFTCard';
import { UserInfoType } from './UserInfo';
import LetterCard from '../../commons/LetterCard';
import { ConsonantOrder, VowelOrder } from '../../_store/store';

const StyledCardList = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  > * {
    width: 290px;
    margin: 2px;
  }
`;

const StyledNFTList = styled.div`
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(4, minmax(277px, auto));
  > * {
    text-decoration: none;
  }
`;

export default function UserCardList({ walletAddress }: UserInfoType) {
  const dispatch = useDispatch();

  const [showArr, setShowArr] = useState<NFT[]>([]);
  const [label, setLabel] = useState<String>('collected');

  const handleClick = (arr: Array<NFT>, Lable: String) => {
    if (Lable !== 'inventory') {
      setShowArr(arr);
    }
    setLabel(Lable);
  };

  const collected = useSelector((state: any) => state.user.collected);
  const created = useSelector((state: any) => state.user.created);
  const onsale = useSelector((state: any) => state.user.onsale);
  const favorite = useSelector((state: any) => state.user.favorite);
  const inventory = useSelector((state: any) => state.user.inventory);

  useEffect(() => {
    if (walletAddress) {
      dispatch(fetchCollected(walletAddress));
      dispatch(fetchCreated(walletAddress));
      dispatch(fetchFavorite(walletAddress));
      dispatch(fetchOnsale(walletAddress));
      dispatch(fetchInventory(walletAddress));
    }
  }, [walletAddress]);

  useEffect(() => {
    setShowArr(collected);
  }, [collected]);
  return (
    <>
      {/* <div>collected</div> */}
      <ButtonGroup variant="contained" aria-label="text primary button group" fullWidth sx={{ margin: '10px 0' }}>
        <Button onClick={() => handleClick(collected, 'collected')}>Collected</Button>
        <Button onClick={() => handleClick(created, 'created')}>Created</Button>
        <Button onClick={() => handleClick(onsale, 'onsale')}>Onsale</Button>
        <Button onClick={() => handleClick(favorite, 'favorite')}>Favorite</Button>
        <Button onClick={() => handleClick([], 'inventory')}>Inventory</Button>
      </ButtonGroup>
      {label === 'inventory' ? (
        <StyledCardList>
          {Object.values<Array<Hangul>>(inventory).map((one: Array<Hangul>, idx: Number) =>
            one.map((hangul: Hangul, index) =>
              hangul.quantity > 0 ? (
                hangul.hangulId <= 30 ? (
                  <LetterCard
                    description={hangul.description}
                    title={hangul.title}
                    letter={`${ConsonantOrder[index]}`}
                    quantity={hangul.quantity}
                    key={'inventory' + `${idx}` + `${index}`}
                  />
                ) : (
                  <LetterCard
                    description={hangul.description}
                    title={hangul.title}
                    letter={VowelOrder[index]}
                    quantity={hangul.quantity}
                    key={'inventory' + `${idx}` + `${index}`}
                  />
                )
              ) : null
            )
          )}
        </StyledCardList>
      ) : (
        <StyledNFTList>
          {showArr.map((NFTInfo: NFT, idx: Number) => (
            <Link to={`/nftdetail/${NFTInfo.nftAddress}`} key={`${idx}` + NFTInfo.imgUrl}>
              <NFTCard
                imgUrl={NFTInfo.imgUrl}
                nftTitle={NFTInfo.nftTitle}
                nftPrice={NFTInfo.nftPrice}
                nftCreatorNickname={NFTInfo.nftCreatorNickname}
                lastPrice={NFTInfo.lastPrice}
                nftOwnerNickname={NFTInfo.nftOwnerNickname}
                nftTags={NFTInfo.nftTags}
                nftLike={NFTInfo.nftLike}
                nftAddress={NFTInfo.nftAddress}
                nftDescription={NFTInfo.nftDescription}
                tokenId={NFTInfo.tokenId}
                isSale={NFTInfo.isSale}
              />
            </Link>
          ))}
        </StyledNFTList>
      )}
    </>
  );
}
