import { ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

export default function About_App() {
  return (
    <SafeAreaView style={styles.safeContainer}>
      <ScrollView
        contentContainerStyle={styles.container}
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.title}>About AR Filter App</Text>

        <Text style={styles.description}>
          AR Filter App is a real-time augmented reality mobile application
          developed using React Native and Expo.
        </Text>

        <Text style={styles.description}>
          The application allows users to open the camera, choose interactive
          face filters, apply them live like social media apps, capture photos,
          and save the final result directly to the device.
        </Text>

        <Text style={styles.description}>
          The project focuses on creating a smooth and modern AR experience
          inspired by popular camera filter applications such as Snapchat and
          Instagram.
        </Text>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Features</Text>

          <Text style={styles.item}>• Live camera filters</Text>
          <Text style={styles.item}>• Real-time AR effects</Text>
          <Text style={styles.item}>• Capture and save photos</Text>
          <Text style={styles.item}>• Interactive modern UI</Text>
          <Text style={styles.item}>• Multiple filter selection</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Developed By</Text>

          <Text style={styles.item}>• Malak Ahmed</Text>
          <Text style={styles.item}>• Fatma Nazih</Text>
          <Text style={styles.item}>• Mostafa Sobhy</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeContainer: {
    flex: 1,
    backgroundColor: "#0F1115",
  },

  container: {
    padding: 25,
    paddingBottom: 40,
  },

  title: {
    color: "white",
    fontSize: 30,
    fontWeight: "bold",
    marginBottom: 25,
    textAlign: "center",
  },

  description: {
    color: "#D1D1D1",
    fontSize: 16,
    lineHeight: 24,
    marginBottom: 15,
    textAlign: "center",
  },

  section: {
    marginTop: 25,
  },

  sectionTitle: {
    color: "#6C5CE7",
    fontSize: 22,
    fontWeight: "bold",
    marginBottom: 12,
  },

  item: {
    color: "white",
    fontSize: 17,
    marginBottom: 8,
  },
});