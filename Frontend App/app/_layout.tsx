import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import 'react-native-reanimated';

import ImageProvider from '@/context/ImageContext';
import { useColorScheme } from '@/hooks/use-color-scheme';
import IsLoggedInProvider from '@/context/IsLoggedIn';

export default function RootLayout() {
  const colorScheme = useColorScheme();

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <ImageProvider>
        <IsLoggedInProvider>
            <Stack
              screenOptions={{
                headerStyle: {
                  backgroundColor: "#191d25",
                },
                headerTintColor: "#fff",
                headerTitleStyle: {
                  fontWeight: "bold",
                },
                headerShown: true,
              }}
            >
            </Stack>
          <StatusBar style="auto" />

        </IsLoggedInProvider>
      </ImageProvider>
    </ThemeProvider>
  );
}
