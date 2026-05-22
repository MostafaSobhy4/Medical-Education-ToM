import cv2
import numpy as np
import mediapipe as mp

print(mp)
print(mp.__file__)

mp_face_mesh = mp.solutions.face_mesh
face_mesh = mp_face_mesh.FaceMesh(refine_landmarks=True)

mp_selfie_segmentation = mp.solutions.selfie_segmentation

selfie_segmentation = mp_selfie_segmentation.SelfieSegmentation(
    model_selection=1
)

# Landmarks for eyes (MediaPipe FaceMesh)
LEFT_EYE = [33, 133, 160, 159, 158, 153]
RIGHT_EYE = [362, 385, 387, 386, 374, 373]

FACE_CONTOUR = [
    10, 338, 297, 332, 284, 251,
    389, 356, 454, 323, 361, 288,
    397, 365, 379, 378, 400, 377,
    152,
    148, 176, 149, 150, 136, 172,
    58, 132, 93, 234, 127, 162,
    21, 54, 103, 67, 109
]

def apply_jaundice(frame, segmentation_mask, landmarks=None):

    h, w, _ = frame.shape

    # =========================
    # CREATE HUMAN MASK
    # =========================

    mask = (segmentation_mask > 0.35).astype(np.uint8) * 255

    # smooth edges
    mask = cv2.GaussianBlur(mask, (15, 15), 0)

    # =========================
    # CREATE YELLOW TINT
    # =========================

    yellow = np.full_like(frame, (0, 255, 255))

    tinted = cv2.addWeighted(frame, 0.72, yellow, 0.28, 0)

    # =========================
    # APPLY ONLY TO PERSON
    # =========================

    mask_3ch = cv2.cvtColor(mask, cv2.COLOR_GRAY2BGR)

    mask_float = mask_3ch.astype(np.float32) / 255.0

    result = (
        frame.astype(np.float32) * (1 - mask_float)
        + tinted.astype(np.float32) * mask_float
    )

    result = result.astype(np.uint8)

    # =========================
    # EXTRA YELLOW EYES
    # =========================

    if landmarks is not None:

        for idx in LEFT_EYE + RIGHT_EYE:

            x = int(landmarks[idx].x * w)
            y = int(landmarks[idx].y * h)

            cv2.circle(result, (x, y), 3, (0, 255, 255), -1)

    return result

def apply_edema(frame, landmarks):
    h, w, _ = frame.shape

    # create puffiness mask around both eyes
    for eye in [LEFT_EYE, RIGHT_EYE]:
        pts = []
        for idx in eye:
            x = int(landmarks[idx].x * w)
            y = int(landmarks[idx].y * h)
            pts.append([x, y])

        pts = np.array(pts, np.int32)

        # expand region slightly (simulate swelling)
        x, y, bw, bh = cv2.boundingRect(pts)
        x -= 10
        y -= 10
        bw += 20
        bh += 20

        roi = frame[y:y+bh, x:x+bw]
        if roi.size == 0:
            continue

        # blur = swelling/puffy skin effect
        blurred = cv2.GaussianBlur(roi, (35, 35), 30)
        frame[y:y+bh, x:x+bw] = cv2.addWeighted(roi, 0.3, blurred, 0.7, 0)

    return frame


def main():
    print("Choose filter:")
    print("1 -> Jaundice (yellow skin/eyes)")
    print("2 -> Edema (puffy eyes)")

    choice = input("Enter 1 or 2: ")

    cap = cv2.VideoCapture(0)

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        result = face_mesh.process(rgb)
        segmentation_result = selfie_segmentation.process(rgb)
        segmentation_mask = segmentation_result.segmentation_mask

        if result.multi_face_landmarks:
            landmarks = result.multi_face_landmarks[0].landmark

            if choice == "1":
                frame = apply_jaundice(
                frame,
                segmentation_mask,
                landmarks
            )

            elif choice == "2":
                frame = apply_edema(frame, landmarks)
            

        cv2.imshow("AR Medical Filter", frame)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()


if __name__ == "__main__":
    main()