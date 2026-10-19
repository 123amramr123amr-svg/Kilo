/**
 * Permissions Screen - Request camera, microphone, and media permissions
 */

import React, { useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { MaterialCommunityIcons } from 'react-native-vector-icons';
import { usePermissions } from '../hooks/usePermissions';
import { COLORS, SPACING, BORDER_RADIUS } from '../constants/theme';

export const PermissionsScreen = ({ navigation }: any) => {
  const { permissions, requestAll } = usePermissions();

  useEffect(() => {
    requestAll();
  }, []);

  const allGranted =
    permissions.camera === 'granted' &&
    permissions.microphone === 'granted' &&
    permissions.mediaLibrary === 'granted';

  if (allGranted) {
    return null;
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <MaterialCommunityIcons name="shield-check-outline" size={64} color={COLORS.primary} />
        <Text style={styles.title}>إذن الوصول</Text>
        <Text style={styles.subtitle}>
          يحتاج LensAI إلى أذونات الكاميرة والميكروفون والمكتبة لالتقاط الصور والفيديو وحفظها.
        </Text>
      </View>

      <View style={styles.permissionsList}>
        <View style={styles.permissionItem}>
          <MaterialCommunityIcons
            name="camera-outline"
            size={24}
            color={permissions.camera === 'granted' ? COLORS.success : COLORS.secondary}
          />
          <View style={styles.permissionTextContainer}>
            <Text style={styles.permissionLabel}>الكاميرة</Text>
            <Text style={[styles.permissionStatus, { color: permissions.camera === 'granted' ? COLORS.success : COLORS.warning }]}>
              {permissions.camera === 'granted' ? 'تم منح الإذن' : 'ينتظر الإذن'}
            </Text>
          </View>
        </View>
        <View style={styles.permissionItem}>
          <MaterialCommunityIcons
            name="microphone-outline"
            size={24}
            color={permissions.microphone === 'granted' ? COLORS.success : COLORS.secondary}
          />
          <View style={styles.permissionTextContainer}>
            <Text style={styles.permissionLabel}>الميكروفون</Text>
            <Text style={[styles.permissionStatus, { color: permissions.microphone === 'granted' ? COLORS.success : COLORS.warning }]}>
              {permissions.microphone === 'granted' ? 'تم منح الإذن' : 'ينتظر الإذن'}
            </Text>
          </View>
        </View>
        <View style={styles.permissionItem}>
          <MaterialCommunityIcons
            name="image-outline"
            size={24}
            color={permissions.mediaLibrary === 'granted' ? COLORS.success : COLORS.secondary}
          />
          <View style={styles.permissionTextContainer}>
            <Text style={styles.permissionLabel}>معرض الصور</Text>
            <Text style={[styles.permissionStatus, { color: permissions.mediaLibrary === 'granted' ? COLORS.success : COLORS.warning }]}>
              {permissions.mediaLibrary === 'granted' ? 'تم منح الإذن' : 'ينتظر الإذن'}
            </Text>
          </View>
        </View>
      </View>

      <TouchableOpacity style={styles.requestButton} onPress={requestAll}>
        <MaterialCommunityIcons name="refresh" size={20} color={COLORS.text.primary} />
        <Text style={styles.requestButtonText}>طلب الأذونات مرة أخرى</Text>
      </TouchableOpacity>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.background,
    alignItems: 'center',
    justifyContent: 'center',
    padding: SPACING.lg,
  },
  header: {
    alignItems: 'center',
    marginBottom: SPACING.xl,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: COLORS.text.primary,
    marginTop: SPACING.lg,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 15,
    color: COLORS.text.secondary,
    textAlign: 'center',
    marginTop: SPACING.md,
    lineHeight: 24,
  },
  permissionsList: {
    width: '100%',
    gap: SPACING.md,
  },
  permissionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: COLORS.surface,
    padding: SPACING.md,
    borderRadius: BORDER_RADIUS.lg,
    borderWidth: 1,
    borderColor: COLORS.border,
  },
  permissionTextContainer: {
    flex: 1,
    marginLeft: SPACING.md,
  },
  permissionLabel: {
    fontSize: 15,
    fontWeight: '600',
    color: COLORS.text.primary,
  },
  permissionStatus: {
    fontSize: 13,
    color: COLORS.text.tertiary,
    marginTop: 2,
  },
  requestButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: COLORS.primary,
    paddingVertical: SPACING.md,
    paddingHorizontal: SPACING.xl,
    borderRadius: BORDER_RADIUS.full,
    marginTop: SPACING.xl,
  },
  requestButtonText: {
    marginLeft: SPACING.sm,
    color: COLORS.text.primary,
    fontSize: 15,
    fontWeight: '600',
  },
});
