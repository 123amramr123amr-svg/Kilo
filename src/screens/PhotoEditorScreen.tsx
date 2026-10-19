/**
 * Photo Editor Screen
 */

import React, { useState } from 'react';
import { View, Text, Image, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { useApp } from '../context/AppContext';
import { COLORS, SPACING, BORDER_RADIUS } from '../constants/theme';

export const PhotoEditorScreen = ({ route, navigation }: any) => {
  const { photoId } = route.params;
  const { photos, updatePhoto } = useApp();
  const photo = photos.find(p => p.id === photoId);

  const [brightness, setBrightness] = useState(0);
  const [contrast, setContrast] = useState(0);
  const [saturation, setSaturation] = useState(0);
  const [sharpness, setSharpness] = useState(0);
  const [showComparison, setShowComparison] = useState(false);

  const handleSave = () => {
    if (!photo) return;
    updatePhoto(photo.id, {
      isEdited: true,
      edits: { brightness, contrast, saturation, sharpness },
    });
    navigation.goBack();
  };

  if (!photo) {
    return (
      <View style={styles.container}>
        <Text style={styles.error}>لم يتم العثور على الصورة</Text>
      </View>
    );
  }

  const sliderItems = [
    { label: 'الإضاءة', value: brightness, setter: setBrightness },
    { label: 'التباين', value: contrast, setter: setContrast },
    { label: 'التشبع', value: saturation, setter: setSaturation },
    { label: 'الحدة', value: sharpness, setter: setSharpness },
  ];

  return (
    <View style={styles.container}>
      <View style={styles.imageContainer}>
        <Image source={{ uri: photo.uri }} style={styles.image} resizeMode="contain" />
        {showComparison && (
          <View style={styles.comparisonOverlay}>
            <Text style={styles.comparisonText}>قبل التعديل</Text>
          </View>
        )}
      </View>
      <View style={styles.controls}>
        <TouchableOpacity style={styles.compareButton} onPress={() => setShowComparison(!showComparison)}>
          <Text style={styles.compareText}>{showComparison ? 'إخفاء المقارنة' : 'مقارنة قبل/後'}</Text>
        </TouchableOpacity>
        <ScrollView style={styles.sliders} showsVerticalScrollIndicator={false}>
          {sliderItems.map(item => (
            <View key={item.label} style={styles.sliderRow}>
              <Text style={styles.sliderLabel}>{item.label}</Text>
              <View style={styles.sliderTrack}>
                <View style={[styles.sliderFill, { width: `${((item.value + 100) / 200) * 100}%` }]} />
              </View>
              <Text style={styles.sliderValue}>{item.value}</Text>
              <TouchableOpacity style={styles.sliderButton} onPress={() => item.setter(Math.max(-100, item.value - 5))}>
                <Text style={styles.sliderButtonText}>-</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.sliderButton} onPress={() => item.setter(Math.min(100, item.value + 5))}>
                <Text style={styles.sliderButtonText}>+</Text>
              </TouchableOpacity>
            </View>
          ))}
        </ScrollView>
      </View>
      <View style={styles.footer}>
        <TouchableOpacity style={styles.cancelButton} onPress={() => navigation.goBack()}>
          <Text style={styles.cancelText}>إلغاء</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.saveButton} onPress={handleSave}>
          <Text style={styles.saveText}>حفظ</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.background },
  imageContainer: { flex: 1, backgroundColor: COLORS.surface },
  image: { width: '100%', height: '100%' },
  comparisonOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'center', alignItems: 'center' },
  comparisonText: { color: COLORS.text.primary, fontSize: 18, fontWeight: '600' },
  controls: { padding: SPACING.md, backgroundColor: COLORS.backgroundElevated },
  compareButton: { padding: SPACING.sm, backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, alignItems: 'center', marginBottom: SPACING.md },
  compareText: { color: COLORS.text.primary, fontSize: 14, fontWeight: '600' },
  sliders: { maxHeight: 200 },
  sliderRow: { flexDirection: 'row', alignItems: 'center', marginBottom: SPACING.md, gap: SPACING.sm },
  sliderLabel: { color: COLORS.text.primary, fontSize: 14, width: 60, textAlign: 'right' },
  sliderTrack: { flex: 1, height: 4, backgroundColor: COLORS.border, borderRadius: 2, overflow: 'hidden' },
  sliderFill: { height: '100%', backgroundColor: COLORS.primary },
  sliderValue: { color: COLORS.text.secondary, fontSize: 12, width: 30, textAlign: 'center' },
  sliderButton: { width: 32, height: 32, borderRadius: 16, backgroundColor: COLORS.surface, justifyContent: 'center', alignItems: 'center' },
  sliderButtonText: { color: COLORS.text.primary, fontSize: 16, fontWeight: '700' },
  footer: { flexDirection: 'row', padding: SPACING.md, gap: SPACING.md },
  cancelButton: { flex: 1, padding: SPACING.md, backgroundColor: COLORS.surface, borderRadius: BORDER_RADIUS.md, alignItems: 'center' },
  cancelText: { color: COLORS.text.primary, fontSize: 16 },
  saveButton: { flex: 1, padding: SPACING.md, backgroundColor: COLORS.primary, borderRadius: BORDER_RADIUS.md, alignItems: 'center' },
  saveText: { color: COLORS.text.primary, fontSize: 16, fontWeight: '600' },
  error: { color: COLORS.error, fontSize: 16, textAlign: 'center', marginTop: 100 },
});
