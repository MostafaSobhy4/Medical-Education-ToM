import { Ionicons } from "@expo/vector-icons";
import { useState } from "react";
import { TextInput, TouchableOpacity, View } from "react-native";

export default function PasswordInput({ value, onChangeText, placeholder }: any) {

  const [showPassword, setShowPassword] = useState(false);

  return (
    <View style={{ position: "relative", width: "100%" }}>

      <TextInput
        placeholder={placeholder}
        placeholderTextColor="#999"
        secureTextEntry={!showPassword}
        value={value}
        onChangeText={onChangeText}
        style={{
          backgroundColor: "#191d25",
          color: "white",
          padding: 12,
          borderRadius: 8,
          paddingRight: 40,
        }}
      />

      <TouchableOpacity
        onPress={() => setShowPassword(!showPassword)}
        style={{
          position: "absolute",
          right: 10,
          top: 12,
        }}
      >
        <Ionicons
          name={showPassword ? "eye" : "eye-off"}
          size={22}
          color="white"
        />
      </TouchableOpacity>

    </View>
  );
}
