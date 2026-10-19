/**
 * Gallery Screen - view and manage photos
 */

import React, { useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, Image } from 'react-native';
import { useApp } from '../context/AppContext';
import { COLORS, SPACING, BORDER_RADIUS } from '../constants/theme';
import { PhotoInfo } from '../types';

export const GalleryScreen = ({ navigation }: any) => {
  const { photos, toggleFavorite } = useApp();
  const [selectedTab, setSelectedTab] = useState<'all' | 'favorites'>('all');

  const filteredPhotos = selectedTab === 'favorites'
    ? photos.filter(p => p.isFavorite)
    : photos;

  const renderItem = ({ item }: { item: PhotoInfo }) => (
    <TouchableOpacity
      style={styles.photoItem}
      onPress={() => navigation.navigate('PhotoDetail', { photoId: item.id })}
    >
      <Image source={{ uri: item.uri }} style={styles.photoThumb} />
      <TouchableOpacity
        style={styles.favoriteButton}
        onPress={() => toggleFavorite(item.id)}
      >
        <Text style={styles.favoriteIcon}>{item.isFavorite ? '❤️' : '🤍'}</Text>
      </TouchableOpacity>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>المعرض</Text>
        <View style={styles.tabs}>
          <TouchableOpacity
            style={[styles.tab, selectedTab === 'all' && styles.tabActive]}
            onPress={() => setSelectedTab('all')}
          >
            <Text style={[styles.tabText, selectedTab === 'all' && styles.tabTextActive]}>
              الكل ({photos.length})
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.tab, selectedTab === 'favorites' && styles.tabActive]}
            onPress={() => setSelectedTab('favorites')}
          >
            <Text style={[styles.tabText, selectedTab === 'favorites' && styles.tabTextActive]}>
              المفضلة ({photos.filter(p => p.isFavorite).length})
            </Text>
          </TouchableOpacity>
        </View>
      </View>
      {filteredPhotos.length === 0 ? (
        <View style={styles.emptyState}>
          <Text style={styles.emptyIcon}>📷</Text>
          <Text style={styles.emptyText}>لا توجد صور بعد</Text>
          <Text style={styles.emptySubtext}>ابدأ بأخذ صورك الأولى!</Text>
        </View>
      ) : (
        <FlatList
          data={filteredPhotos}
          renderItem={renderItem}
          keyExtractor={item => item.id}
          numColumns={3}
          contentContainerStyle={styles.grid}
        />
      )}
      <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
        <Text style={styles.backButtonText}>← رجوع</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.background },
  header: { paddingTop: 60, paddingHorizontal: SPACING.md, paddingBottom: SPACING.md },
  title: { fontSize: 28, fontWeight: '700', color: COLORS.text.primary, marginBottom: SPACING.md, textAlign: 'right' },
  tabs: { flexDirection: 'row', gap: SPACING.sm },
  tab: { paddingHorizontal: SPACING.md, paddingVertical: SPACING.sm, borderRadius: BORDER_RADIUS.full, backgroundColor: COLORS.surface },
  tabActive: { backgroundColor: COLORS.primary },
  tabText: { color: COLORS.text.secondary, fontSize: 14 },
  tabTextActive: { color: COLORS.text.primary, fontWeight: '600' },
  grid: { paddingHorizontal: SPACING.xs },
  photoItem: { width: '33.33%', aspectRatio: 1, padding: 2 },
  photoThumb: { width: '100%', height: '100%', borderRadius: 8, backgroundColor: COLORS.surface },
  favoriteButton: { position: 'absolute', top: 4, right: 4, padding: 4 },
  favoriteIcon: { fontSize: 16 },
  emptyState: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyIcon: { fontSize: 64, marginBottom: SPACING.md },
  emptyText: { color: COLORS.text.primary, fontSize: 20, fontWeight: '600', marginBottom: SPACING.xs },
  emptySubtext: { color: COLORS.text.secondary, fontSize: 14 },
  backButton: { position: 'absolute', bottom: 30, left: SPACING.md, right: SPACING.md, padding: SPACING.md, backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, alignItems: 'center' },
  backButtonText: { color: COLORS.text.primary, fontSize: 16, fontWeight: '600' },
});
