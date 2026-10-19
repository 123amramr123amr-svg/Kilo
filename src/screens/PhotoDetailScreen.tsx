/**
 * Photo Detail Screen
 */

import React from 'react';
import { View, Text, Image, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { useApp } from '../context/AppContext';
import { COLORS, SPACING, BORDER_RADIUS } from '../constants/theme';
import { formatDateTime } from '../utils/helpers';

export const PhotoDetailScreen = ({ route, navigation }: any) => {
  const { photoId } = route.params;
  const { photos, toggleFavorite, deletePhoto } = useApp();
  const photo = photos.find(p => p.id === photoId);

  if (!photo) {
    return (
      <View style={styles.container}>
        <Text style={styles.error}>لم يتم العثور على الصورة</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Image source={{ uri: photo.uri }} style={styles.image} resizeMode="contain" />
      <View style={styles.actions}>
        <TouchableOpacity style={styles.actionButton} onPress={() => toggleFavorite(photo.id)}>
          <Text style={styles.actionIcon}>{photo.isFavorite ? '❤️' : '🤍'}</Text>
          <Text style={styles.actionLabel}>المفضلة</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionButton} onPress={() => navigation.navigate('PhotoEditor', { photoId: photo.id })}>
          <Text style={styles.actionIcon}>✏️</Text>
          <Text style={styles.actionLabel}>تعديل</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionButton} onPress={() => navigation.navigate('AiSuggestions', { photoId: photo.id })}>
          <Text style={styles.actionIcon}>✨</Text>
          <Text style={styles.actionLabel}>اقتراحات AI</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionButton} onPress={() => { deletePhoto(photo.id); navigation.goBack(); }}>
          <Text style={styles.actionIcon}>🗑️</Text>
          <Text style={styles.actionLabel}>حذف</Text>
        </TouchableOpacity>
      </View>
      <ScrollView style={styles.info}>
        <View style={styles.infoRow}>
          <Text style={styles.infoLabel}>التاريخ</Text>
          <Text style={styles.infoValue}>{formatDateTime(photo.timestamp)}</Text>
        </View>
        <View style={styles.infoRow}>
          <Text style={styles.infoLabel}>الوضع</Text>
          <Text style={styles.infoValue}>{photo.mode}</Text>
        </View>
        <View style={styles.infoRow}>
          <Text style={styles.infoLabel}>الحجم</Text>
          <Text style={styles.infoValue}>{photo.width} × {photo.height}</Text>
        </View>
      </ScrollView>
      <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
        <Text style={styles.backButtonText}>← رجوع</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.background },
  image: { width: '100%', height: 300, backgroundColor: COLORS.surface },
  actions: { flexDirection: 'row', justifyContent: 'space-around', padding: SPACING.md, backgroundColor: COLORS.surface },
  actionButton: { alignItems: 'center', gap: SPACING.xs },
  actionIcon: { fontSize: 24 },
  actionLabel: { color: COLORS.text.secondary, fontSize: 12 },
  info: { padding: SPACING.md },
  infoRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: SPACING.sm, borderBottomWidth: 1, borderBottomColor: COLORS.borderLight },
  infoLabel: { color: COLORS.text.secondary, fontSize: 14 },
  infoValue: { color: COLORS.text.primary, fontSize: 14, fontWeight: '600' },
  error: { color: COLORS.error, fontSize: 16, textAlign: 'center', marginTop: 100 },
  backButton: { margin: SPACING.md, padding: SPACING.md, backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, alignItems: 'center' },
  backButtonText: { color: COLORS.text.primary, fontSize: 16, fontWeight: '600' },
});
