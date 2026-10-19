/**
 * Camera Screen - main camera view
 */

import React, { useState, useCallback } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Dimensions } from 'react-native';
import { CameraView } from 'expo-camera';
import * as Haptics from 'expo-haptics';
import { useCamera } from '../hooks/useCamera';
import { useApp } from '../context/AppContext';
import { COLORS, SPACING, BORDER_RADIUS, CAMERA_MODES } from '../constants/theme';
import { CameraMode } from '../types';

const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get('window');

export const CameraScreen = ({ navigation }: any) => {
  const camera = useCamera();
  const { settings, updateSettings, addPhoto } = useApp();
  const [mode, setMode] = useState<CameraMode>('photo');
  const [showGrid, setShowGrid] = useState(settings.gridEnabled);
  const [lastPhotoUri, setLastPhotoUri] = useState<string | null>(null);

  const handleCapture = useCallback(async () => {
    if (settings.hapticsEnabled) {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
    }
    if (mode === 'photo') {
      const uri = await camera.takePhoto();
      if (uri) {
        setLastPhotoUri(uri);
        addPhoto({
          id: `photo_${Date.now()}`,
          uri,
          width: SCREEN_WIDTH,
          height: SCREEN_HEIGHT,
          timestamp: Date.now(),
          mode,
          isEdited: false,
          isFavorite: false,
        });
      }
    } else if (mode === 'video') {
      if (camera.isRecording) {
        await camera.stopRecording();
      } else {
        await camera.startRecording();
      }
    }
  }, [mode, camera, settings, addPhoto]);

  return (
    <View style={styles.container}>
      <CameraView
        ref={camera.cameraRef}
        style={styles.preview}
        facing={camera.type}
      />
      {showGrid && (
        <View style={styles.gridOverlay} pointerEvents="none">
          <View style={styles.gridLineH} />
          <View style={[styles.gridLineH, { top: '66%' }]} />
          <View style={styles.gridLineV} />
          <View style={[styles.gridLineV, { left: '66%' }]} />
        </View>
      )}
      <View style={styles.topBar}>
        <TouchableOpacity
          style={styles.iconButton}
          onPress={() => navigation.navigate('Settings')}
        >
          <Text style={styles.iconText}>⚙️</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.iconButton}
          onPress={() => camera.setFlash(camera.flash === 'off' ? 'on' : 'off')}
        >
          <Text style={styles.iconText}>⚡</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.iconButton}
          onPress={() => updateSettings({ aiAssistantEnabled: !settings.aiAssistantEnabled })}
        >
          <Text style={styles.iconText}>✨</Text>
        </TouchableOpacity>
      </View>
      <View style={styles.modeSelector}>
        {(['photo', 'video', 'pro'] as CameraMode[]).map(m => (
          <TouchableOpacity
            key={m}
            style={[styles.modeButton, mode === m && styles.modeButtonActive]}
            onPress={() => setMode(m)}
          >
            <Text style={[styles.modeText, mode === m && styles.modeTextActive]}>
              {CAMERA_MODES[m].labelAr}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
      <View style={styles.bottomBar}>
        <TouchableOpacity
          style={styles.galleryButton}
          onPress={() => navigation.navigate('Gallery')}
        >
          <View style={styles.galleryThumb}>
            {lastPhotoUri ? <View style={styles.galleryThumbInner} /> : null}
          </View>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.shutterButton, mode === 'video' && camera.isRecording && styles.shutterRecording]}
          onPress={handleCapture}
        />
        <TouchableOpacity style={styles.flipButton} onPress={camera.toggleCamera}>
          <Text style={styles.flipText}>🔄</Text>
        </TouchableOpacity>
      </View>
      {settings.aiAssistantEnabled && (
        <View style={styles.aiBar}>
          <Text style={styles.aiText}>✨ مساعد AI: جرب التصوير من مستوى أقل</Text>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.background },
  preview: { flex: 1 },
  gridOverlay: { ...StyleSheet.absoluteFillObject },
  gridLineH: {
    position: 'absolute', left: 0, right: 0, top: '33%', height: 1,
    backgroundColor: COLORS.gridLine,
  },
  gridLineV: {
    position: 'absolute', top: 0, bottom: 0, left: '33%', width: 1,
    backgroundColor: COLORS.gridLine,
  },
  topBar: {
    position: 'absolute', top: 50, left: 0, right: 0,
    flexDirection: 'row', justifyContent: 'space-between', paddingHorizontal: SPACING.md,
  },
  iconButton: {
    width: 44, height: 44, borderRadius: 22, backgroundColor: COLORS.overlay,
    justifyContent: 'center', alignItems: 'center',
  },
  iconText: { fontSize: 20 },
  modeSelector: {
    position: 'absolute', bottom: 140, left: 0, right: 0,
    flexDirection: 'row', justifyContent: 'center', gap: SPACING.lg,
  },
  modeButton: { paddingHorizontal: SPACING.lg, paddingVertical: SPACING.sm, borderRadius: BORDER_RADIUS.full },
  modeButtonActive: { backgroundColor: 'rgba(255,255,255,0.2)' },
  modeText: { color: COLORS.text.secondary, fontSize: 14, fontWeight: '500' },
  modeTextActive: { color: COLORS.text.primary, fontWeight: '700' },
  bottomBar: {
    position: 'absolute', bottom: 50, left: 0, right: 0,
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-around',
    paddingHorizontal: SPACING.xl,
  },
  galleryButton: {
    width: 56, height: 56, borderRadius: 12, backgroundColor: COLORS.surface,
    justifyContent: 'center', alignItems: 'center', borderWidth: 2, borderColor: COLORS.border,
  },
  galleryThumb: { width: 44, height: 44, borderRadius: 8, backgroundColor: COLORS.surfaceHover },
  galleryThumbInner: { width: '100%', height: '100%', borderRadius: 6, backgroundColor: COLORS.primary },
  shutterButton: {
    width: 76, height: 76, borderRadius: 38, backgroundColor: COLORS.shutter,
    borderWidth: 4, borderColor: 'rgba(255,255,255,0.3)',
  },
  shutterRecording: { backgroundColor: COLORS.error },
  flipButton: {
    width: 56, height: 56, borderRadius: 28, backgroundColor: COLORS.overlay,
    justifyContent: 'center', alignItems: 'center',
  },
  flipText: { fontSize: 24 },
  aiBar: { position: 'absolute', top: 110, left: 0, right: 0, alignItems: 'center' },
  aiText: {
    backgroundColor: COLORS.overlay, color: COLORS.text.primary,
    paddingHorizontal: SPACING.md, paddingVertical: SPACING.xs,
    borderRadius: BORDER_RADIUS.full, fontSize: 13,
  },
});
