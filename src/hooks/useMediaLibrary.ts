/**
 * Media Library hook - save and load photos/videos
 */

import { useState } from 'react';
import * as MediaLibrary from 'expo-media-library';

export const useMediaLibrary = () => {
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);

  const requestPermission = async () => {
    const { status } = await MediaLibrary.requestPermissionsAsync();
    setHasPermission(status === 'granted');
    return status === 'granted';
  };

  const savePhoto = async (uri: string): Promise<string | null> => {
    try {
      const asset = await MediaLibrary.createAssetAsync(uri);
      return asset.id;
    } catch (error) {
      console.error('Failed to save photo', error);
      return null;
    }
  };

  const getPhotos = async (limit: number = 50) => {
    try {
      const { assets } = await MediaLibrary.getAssetsAsync({
        mediaType: MediaLibrary.MediaType.photo,
        first: 50,
        sortBy: ['creationTime'],
      });
      return assets;
    } catch (error) {
      console.error('Failed to load photos', error);
      return [];
    }
  };

  return {
    hasPermission,
    requestPermission,
    savePhoto,
    getPhotos,
  };
};