/**
 * React Context for app settings and state management
 */

import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
  useCallback,
  useMemo,
} from 'react';
import { AppSettings, PhotoInfo, Album, AiSession, VideoInfo } from '../types';
import { DEFAULT_SETTINGS } from '../constants/theme';

interface AppContextType {
  settings: AppSettings;
  updateSettings: (partial: Partial<AppSettings>) => void;
  resetSettings: () => void;
  photos: PhotoInfo[];
  addPhoto: (photo: PhotoInfo) => void;
  updatePhoto: (id: string, partial: Partial<PhotoInfo>) => void;
  deletePhoto: (id: string) => void;
  toggleFavorite: (id: string) => void;
  albums: Album[];
  createAlbum: (name: string, nameAr: string) => void;
  addToAlbum: (albumId: string, photoId: string) => void;
  videos: VideoInfo[];
  addVideo: (video: VideoInfo) => void;
  aiSessions: AiSession[];
  addAiSession: (session: AiSession) => void;
  isCameraReady: boolean;
  setCameraReady: (ready: boolean) => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider = ({ children }: { children: ReactNode }) => {
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS);
  const [photos, setPhotos] = useState<PhotoInfo[]>([]);
  const [albums, setAlbums] = useState<Album[]>([]);
  const [videos, setVideos] = useState<VideoInfo[]>([]);
  const [aiSessions, setAiSessions] = useState<AiSession[]>([]);
  const [isCameraReady, setCameraReady] = useState(false);

  const updateSettings = useCallback((partial: Partial<AppSettings>) => {
    setSettings(prev => ({ ...prev, ...partial }));
  }, []);

  const resetSettings = useCallback(() => {
    setSettings(DEFAULT_SETTINGS);
  }, []);

  const addPhoto = useCallback((photo: PhotoInfo) => {
    setPhotos(prev => [photo, ...prev]);
  }, []);

  const updatePhoto = useCallback((id: string, partial: Partial<PhotoInfo>) => {
    setPhotos(prev =>
      prev.map(p => (p.id === id ? { ...p, ...partial } : p))
    );
  }, []);

  const deletePhoto = useCallback((id: string) => {
    setPhotos(prev => prev.filter(p => p.id !== id));
  }, []);

  const toggleFavorite = useCallback((id: string) => {
    setPhotos(prev =>
      prev.map(p => (p.id === id ? { ...p, isFavorite: !p.isFavorite } : p))
    );
  }, []);

  const createAlbum = useCallback((name: string, nameAr: string) => {
    const album: Album = {
      id: `album_${Date.now()}`,
      name,
      nameAr,
      photoIds: [],
      createdAt: Date.now(),
    };
    setAlbums(prev => [album, ...prev]);
  }, []);

  const addToAlbum = useCallback((albumId: string, photoId: string) => {
    setAlbums(prev =>
      prev.map(a =>
        a.id === albumId && !a.photoIds.includes(photoId)
          ? { ...a, photoIds: [...a.photoIds, photoId] }
          : a
      )
    );
  }, []);

  const addVideo = useCallback((video: VideoInfo) => {
    setVideos(prev => [video, ...prev]);
  }, []);

  const addAiSession = useCallback((session: AiSession) => {
    setAiSessions(prev => [session, ...prev]);
  }, []);

  const value = useMemo(() => ({
    settings,
    updateSettings,
    resetSettings,
    photos,
    addPhoto,
    updatePhoto,
    deletePhoto,
    toggleFavorite,
    albums,
    createAlbum,
    addToAlbum,
    videos,
    addVideo,
    aiSessions,
    addAiSession,
    isCameraReady,
    setCameraReady,
  }), [
    settings,
    updateSettings,
    resetSettings,
    photos,
    addPhoto,
    updatePhoto,
    deletePhoto,
    toggleFavorite,
    albums,
    createAlbum,
    addToAlbum,
    videos,
    addVideo,
    aiSessions,
    addAiSession,
    isCameraReady,
    setCameraReady,
  ]);

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
};

export const useApp = (): AppContextType => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within AppProvider');
  }
  return context;
};